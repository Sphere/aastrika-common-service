package org.aastrika.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aastrika.client.ContentClient;
import org.aastrika.client.UserSearchClient;
import org.aastrika.dto.request.AssessmentSubmitRequest;
import org.aastrika.entity.UserAssessmentMaster;
import org.aastrika.entity.UserAssessmentMasterKey;
import org.aastrika.entity.UserAssessmentSummary;
import org.aastrika.entity.UserAssessmentSummaryKey;
import org.aastrika.entity.UserQuizMaster;
import org.aastrika.entity.UserQuizMasterKey;
import org.aastrika.entity.UserQuizSummary;
import org.aastrika.entity.UserQuizSummaryKey;
import org.aastrika.exception.ApiException;
import org.aastrika.service.AssessmentService;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.datastax.oss.driver.api.core.uuid.Uuids;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AssessmentServiceImpl implements AssessmentService {

    private static final String API_ID = "api.assessment.submit";
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final BigDecimal PASS_PERCENT = BigDecimal.valueOf(60);
    private static final float ASSESSMENT_PASS_SCORE = 60.0f;

    // Scoring keys (question payload)
    private static final String QUESTION_TYPE = "questionType";
    private static final String OPTIONS = "options";
    private static final String OPTION_ID = "optionId";
    private static final String QUESTION_ID = "questionId";
    private static final String IS_CORRECT = "isCorrect";
    private static final String USER_SELECTED = "userSelected";
    private static final String RESPONSE = "response";
    private static final String MTF = "mtf";
    private static final String FITB = "fitb";
    private static final String MCQ_SCA = "mcq-sca";
    private static final String MCQ_MCA = "mcq-mca";

    private final UserSearchClient userSearchClient;
    private final ContentClient contentClient;
    private final CassandraTemplate cassandraTemplate;

    public AssessmentServiceImpl(UserSearchClient userSearchClient, ContentClient contentClient,
                                 CassandraTemplate cassandraTemplate) {
        this.userSearchClient = userSearchClient;
        this.contentClient = contentClient;
        this.cassandraTemplate = cassandraTemplate;
    }

    @Override
    public Map<String, Object> submitAssessment(String rootOrg, String userId, AssessmentSubmitRequest request) {
        // 1) validate user (external user-search — kept as an external call)
        if (!userSearchClient.validateUser(rootOrg, userId)) {
            throw new ApiException(API_ID, HttpStatus.BAD_REQUEST, "Invalid UserId.");
        }

        // 2) score in-memory (the answer key is in the request payload)
        ScoreResult score = validateAssessment(request.getQuestions());

        boolean isAssessment = Boolean.TRUE.equals(request.getIsAssessment());

        // 3) resolve parent + parent content type via the content service hierarchy
        String parentId = contentClient.getParentIdentifier(request.getIdentifier());
        String parentContentType =
                (isAssessment && !parentId.isEmpty()) ? contentClient.getContentType(parentId) : "";

        // 4) persist (assessment or quiz path)
        if (isAssessment) {
            persistAssessment(rootOrg, userId, request, parentId, parentContentType, score);
        } else {
            persistQuiz(rootOrg, userId, request, score);
        }

        // 5) raw score summary (unchanged client contract)
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("result", score.result());
        result.put("correct", score.correct());
        result.put("inCorrect", score.incorrect());
        result.put("blank", score.blank());
        result.put("total", score.correct() + score.incorrect() + score.blank());
        result.put("passPercent", 60);
        return result;
    }

    // ----------------- persistence -----------------

    private void persistAssessment(String rootOrg, String userId, AssessmentSubmitRequest request,
                                   String parentId, String parentContentType, ScoreResult score) {
        Instant now = Instant.now();
        UserAssessmentMaster master = new UserAssessmentMaster();
        master.setKey(new UserAssessmentMasterKey(rootOrg, now, parentId,
                BigDecimal.valueOf(score.result()), Uuids.timeBased()));
        master.setCorrectCount(score.correct());
        master.setDateCreated(startOfDay(now));
        master.setIncorrectCount(score.incorrect());
        master.setNotAnsweredCount(score.blank());
        master.setParentContentType(parentContentType);
        master.setPassPercent(PASS_PERCENT);
        master.setSourceId(request.getIdentifier());
        master.setSourceTitle(request.getTitle());
        master.setUserId(userId);

        UserAssessmentSummary summary = buildSummary(rootOrg, userId, request.getIdentifier(),
                parentContentType, (float) score.result(), now);

        CassandraBatchOperations batch = cassandraTemplate.batchOps();
        batch.insert(master);
        if (summary != null) {
            batch.insert(summary);
        }
        batch.execute();
    }

    /** Summary upsert logic — only for course assessments; null means "don't write the summary". */
    private UserAssessmentSummary buildSummary(String rootOrg, String userId, String contentId,
                                               String parentContentType, float result, Instant now) {
        if (!"course".equalsIgnoreCase(parentContentType)) {
            return null;
        }
        UserAssessmentSummaryKey key = new UserAssessmentSummaryKey(rootOrg, userId, contentId);
        UserAssessmentSummary existing = cassandraTemplate.selectOneById(key, UserAssessmentSummary.class);

        UserAssessmentSummary summary = new UserAssessmentSummary();
        summary.setKey(key);
        summary.setFirstMaxScore(result);
        summary.setFirstMaxScoreDate(now);

        if (existing != null) {
            // only rewrite when this attempt beats the recorded max; keep the original first-pass
            if (existing.getFirstMaxScore() != null && existing.getFirstMaxScore() < result) {
                summary.setFirstPassedScore(existing.getFirstPassedScore());
                summary.setFirstPassedScoreDate(existing.getFirstPassedScoreDate());
                return summary;
            }
            return null;
        }
        // first attempt: record a first-pass only if it passed
        if (result > ASSESSMENT_PASS_SCORE) {
            summary.setFirstPassedScore(result);
            summary.setFirstPassedScoreDate(now);
        }
        return summary;
    }

    private void persistQuiz(String rootOrg, String userId, AssessmentSubmitRequest request, ScoreResult score) {
        Instant now = Instant.now();
        UserQuizMaster quiz = new UserQuizMaster();
        quiz.setKey(new UserQuizMasterKey(rootOrg, now, BigDecimal.valueOf(score.result()), Uuids.timeBased()));
        quiz.setCorrectCount(score.correct());
        quiz.setDateCreated(startOfDay(now));
        quiz.setIncorrectCount(score.incorrect());
        quiz.setNotAnsweredCount(score.blank());
        quiz.setPassPercent(PASS_PERCENT);
        quiz.setSourceId(request.getIdentifier());
        quiz.setSourceTitle(request.getTitle());
        quiz.setUserId(userId);

        UserQuizSummary summary = new UserQuizSummary();
        summary.setKey(new UserQuizSummaryKey(rootOrg, userId, request.getIdentifier()));
        summary.setDateUpdated(now);

        CassandraBatchOperations batch = cassandraTemplate.batchOps();
        batch.insert(quiz);
        batch.insert(summary);
        batch.execute();
    }

    private static Instant startOfDay(Instant now) {
        return LocalDate.ofInstant(now, IST).atStartOfDay(IST).toInstant();
    }

    // ----------------- scoring (answer key is embedded in the request) -----------------

    private record ScoreResult(double result, int correct, int incorrect, int blank) {
    }

    @SuppressWarnings("unchecked")
    private ScoreResult validateAssessment(List<Map<String, Object>> questions) {
        int correct = 0;
        int blank = 0;
        int incorrect = 0;
        Map<String, List<String>> answers = extractAnswers(questions);

        for (Map<String, Object> question : questions) {
            List<String> marked = new ArrayList<>();
            String type = question.containsKey(QUESTION_TYPE) ? String.valueOf(question.get(QUESTION_TYPE)) : null;
            for (Map<String, Object> option : (List<Map<String, Object>>) question.get(OPTIONS)) {
                if (type == null || MCQ_SCA.equals(type) || MCQ_MCA.equals(type)) {
                    if (Boolean.TRUE.equals(option.get(USER_SELECTED))) {
                        marked.add(String.valueOf(option.get(OPTION_ID)));
                    }
                } else if (MTF.equals(type)) {
                    if (hasResponse(option)) {
                        marked.add(option.get(OPTION_ID) + "-" + lower(option.get("text")) + "-"
                                + lower(option.get(RESPONSE)));
                    }
                } else if (FITB.equals(type)) {
                    if (hasResponse(option)) {
                        marked.add(option.get(OPTION_ID) + "-" + lower(option.get(RESPONSE)));
                    }
                }
                // unknown question type: nothing marked -> the question counts as blank (matches source)
            }

            if (marked.isEmpty()) {
                blank++;
            } else {
                List<String> answer = answers.get(String.valueOf(question.get(QUESTION_ID)));
                if (answer != null && answer.size() > 1) {
                    Collections.sort(answer);
                }
                if (marked.size() > 1) {
                    Collections.sort(marked);
                }
                if (marked.equals(answer)) {
                    correct++;
                } else {
                    incorrect++;
                }
            }
        }

        int total = correct + blank + incorrect;
        double result = total == 0 ? 0d : (correct * 100d) / total;
        return new ScoreResult(result, correct, incorrect, blank);
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> extractAnswers(List<Map<String, Object>> questions) {
        Map<String, List<String>> answers = new LinkedHashMap<>();
        for (Map<String, Object> question : questions) {
            List<String> correctOption = new ArrayList<>();
            String type = question.containsKey(QUESTION_TYPE) ? String.valueOf(question.get(QUESTION_TYPE)) : null;
            for (Map<String, Object> option : (List<Map<String, Object>>) question.get(OPTIONS)) {
                if (!Boolean.TRUE.equals(option.get(IS_CORRECT))) {
                    continue;
                }
                if (type == null || MCQ_SCA.equals(type) || MCQ_MCA.equals(type)) {
                    correctOption.add(String.valueOf(option.get(OPTION_ID)));
                } else if (MTF.equals(type)) {
                    correctOption.add(option.get(OPTION_ID) + "-" + lower(option.get("text")) + "-"
                            + lower(option.get("match")));
                } else if (FITB.equals(type)) {
                    correctOption.add(option.get(OPTION_ID) + "-" + lower(option.get("text")));
                }
                // unknown question type: no correct option recorded (matches source default:break)
            }
            answers.put(String.valueOf(question.get(QUESTION_ID)), correctOption);
        }
        return answers;
    }

    private static boolean hasResponse(Map<String, Object> option) {
        return option.containsKey(RESPONSE) && !String.valueOf(option.get(RESPONSE)).isEmpty();
    }

    private static String lower(Object value) {
        return String.valueOf(value).toLowerCase();
    }
}
