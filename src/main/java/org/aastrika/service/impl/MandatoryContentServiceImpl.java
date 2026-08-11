package org.aastrika.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aastrika.client.CourseClient;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.dto.response.MandatoryContentInfo;
import org.aastrika.dto.response.MandatoryContentResponse;
import org.aastrika.entity.MandatoryUserContent;
import org.aastrika.repository.MandatoryUserContentRepository;
import org.aastrika.service.MandatoryContentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Faithful port of the source {@code MandatoryContentServiceImpl.getMandatoryContentStatusForUser}.
 * The column-mapping fragility is gone: mandatory courses are read as a typed
 * {@link MandatoryUserContent} entity (explicit {@code @Column} mappings), so {@code batchId} and
 * {@code minProgressForCompletion} always populate. Each course is enriched with the user's progress
 * from the course service, then {@code mandatoryCourseCompleted} is true only if every course meets
 * its minimum. "No mandatory content configured" is a valid state → HTTP 200 with an explanatory
 * message (not a 404).
 */
@Service
@Slf4j
public class MandatoryContentServiceImpl implements MandatoryContentService {

    private static final String API_ID = "api.mandatory.content.status";
    private static final String MSG_SUCCESS = "Successful";
    private static final String MSG_NO_CONTENT = "No mandatory content configured for this org";

    private final MandatoryUserContentRepository repository;
    private final CourseClient courseClient;

    public MandatoryContentServiceImpl(MandatoryUserContentRepository repository, CourseClient courseClient) {
        this.repository = repository;
        this.courseClient = courseClient;
    }

    @Override
    public AppResponse<Map<String, Object>> getMandatoryContentStatusForUser(String authUserToken, String rootOrg,
            String org, String userId) {
        MandatoryContentResponse response = new MandatoryContentResponse();

        List<MandatoryUserContent> contentList = repository.findByRootOrgAndOrg(rootOrg, org);
        if (contentList.isEmpty()) {
            log.info("getMandatoryContentStatusForUser: no mandatory content set for rootOrg={}, org={}", rootOrg, org);
            return envelope(MSG_NO_CONTENT, response);
        }

        for (MandatoryUserContent content : contentList) {
            MandatoryContentInfo info = new MandatoryContentInfo();
            info.setRootOrg(content.getKey().getRootOrg());
            info.setOrg(content.getKey().getOrg());
            info.setContentType(content.getContentType());
            info.setBatchId(content.getBatchId());
            info.setMinProgressForCompletion(
                    content.getMinProgressForCompletion() == null ? 0.0f : content.getMinProgressForCompletion());
            response.addContentInfo(content.getKey().getContentId(), info);
        }

        enrichProgressDetails(authUserToken, response, userId);

        // Completed only if the user's progress meets the minimum for EVERY mandatory course.
        boolean completed = true;
        for (MandatoryContentInfo info : response.getContentDetails().values()) {
            float progress = info.getUserProgress() == null ? 0.0f : info.getUserProgress();
            float min = info.getMinProgressForCompletion() == null ? 0.0f : info.getMinProgressForCompletion();
            if (progress < min) {
                completed = false;
                break;
            }
        }
        response.setMandatoryCourseCompleted(completed);

        return envelope(MSG_SUCCESS, response);
    }

    /** Fills each course's {@code userProgress} from the course-service progress read (per the source). */
    private void enrichProgressDetails(String authUserToken, MandatoryContentResponse response, String userId) {
        for (Map.Entry<String, MandatoryContentInfo> entry : response.getContentDetails().entrySet()) {
            String courseId = entry.getKey();
            MandatoryContentInfo info = entry.getValue();
            Float progress = courseClient.getCourseProgress(authUserToken, userId, courseId, info.getBatchId());
            if (progress != null) {
                info.setUserProgress(progress);
            }
        }
    }

    private AppResponse<Map<String, Object>> envelope(String message, MandatoryContentResponse response) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", message);
        result.put("response", response);
        return AppResponse.success(API_ID, result, HttpStatus.OK);
    }
}
