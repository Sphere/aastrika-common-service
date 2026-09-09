package org.aastrika.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.aastrika.client.ContentClient;
import org.aastrika.client.CourseClient;
import org.aastrika.client.UserSearchClient;
import org.aastrika.common.Constants;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.dto.response.CohortBatch;
import org.aastrika.dto.response.CohortUsers;
import org.aastrika.dto.response.UserSearchContent;
import org.aastrika.dto.response.UserSearchContent.ProfessionalDetail;
import org.aastrika.exception.ApiException;
import org.aastrika.service.CohortsService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Faithful port of the source {@code CohortsServiceImpl}, covering active-users and auto-enrollment
 * (top-performers was dropped as unused). Behaviour and external calls are preserved, except that
 * active-users no longer NPEs when a resource has no batches — the source passed a null batch list
 * into the participants call; here it returns 404 instead.
 * Status contract: a successful read returns HTTP 200 with the uniform envelope
 * {@code result = {"message": ..., "response": ...}}; "nothing found" returns 404 and a failed
 * course-service write returns 502 (both rendered as the error envelope by the exception handler).
 */
@Service
@Slf4j
public class CohortsServiceImpl implements CohortsService {

    private static final String ACTIVE_USERS_API_ID = "api.cohorts.activeusers";
    private static final String ENROLL_API_ID = "api.autoenrollment";

    private static final String MSG_SUCCESS = "Successful";
    private static final String MSG_NO_BATCHES = "No active batches found for this resource";
    private static final String MSG_NO_ACTIVE_USERS = "No active users found for this resource";
    private static final String MSG_ALREADY_ENROLLED = "User is already enrolled in this course";
    private static final String MSG_ENROLLED = "User enrolled successfully";
    private static final String MSG_BATCH_CREATED_ENROLLED = "Batch created and user enrolled successfully";
    private static final String MSG_CREATE_FAILED = "Failed to create a batch for the course";
    private static final String MSG_ENROLL_FAILED = "Failed to enrol the user into the course";

    private final ContentClient contentClient;
    private final CourseClient courseClient;
    private final UserSearchClient userSearchClient;

    public CohortsServiceImpl(ContentClient contentClient, CourseClient courseClient,
            UserSearchClient userSearchClient) {
        this.contentClient = contentClient;
        this.courseClient = courseClient;
        this.userSearchClient = userSearchClient;
    }

    @Override
    public AppResponse<Map<String, Object>> getActiveUsers(String authUserToken, String rootOrg, String contentId,
            String userId, int count, boolean toFilter) {
        // toFilter is accepted for signature parity; the source never uses it.
        List<String> batchIdList = contentClient.getCourseBatches(contentId).stream()
                .map(CohortBatch::getBatchId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (batchIdList.isEmpty()) {
            // Source would NPE here (null batch list into participants); surface as 404 instead.
            throw new ApiException(ACTIVE_USERS_API_ID, HttpStatus.NOT_FOUND, MSG_NO_BATCHES);
        }
        List<CohortUsers> activeUsers = fetchParticipantsList(authUserToken, rootOrg, batchIdList, count);
        if (activeUsers.isEmpty()) {
            throw new ApiException(ACTIVE_USERS_API_ID, HttpStatus.NOT_FOUND, MSG_NO_ACTIVE_USERS);
        }
        return envelope(ACTIVE_USERS_API_ID, MSG_SUCCESS, activeUsers);
    }

    @Override
    public AppResponse<Map<String, Object>> autoEnrollmentInCourse(String authUserToken, String rootOrg,
            String contentId, String userUUID) {
        List<CohortBatch> batchResp = contentClient.getCourseBatches(contentId);
        List<String> batchIdList = batchResp.stream()
                .map(CohortBatch::getBatchId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (batchIdList.isEmpty()) {
            return createBatchAndEnroll(contentId, userUUID, authUserToken);
        }

        Map<String, CohortBatch> batchMap = new HashMap<>();
        for (CohortBatch batch : batchResp) {
            batchMap.putIfAbsent(batch.getBatchId(), batch);
        }

        // Already enrolled in one of this course's batches? Echo that batch.
        List<String> userBatchIds = courseClient.getUserEnrolledBatchIds(authUserToken, userUUID);
        for (String userBatchId : userBatchIds) {
            if (batchIdList.contains(userBatchId)) {
                return autoEnrollResponse(batchMap.get(userBatchId), MSG_ALREADY_ENROLLED);
            }
        }

        // Otherwise enrol into the first open (no end-date) batch, or create a fresh one.
        for (CohortBatch batch : batchResp) {
            if (isEmpty(batch.getEndDate())) {
                if (!courseClient.enroll(contentId, userUUID, batch.getBatchId(), authUserToken)) {
                    throw new ApiException(ENROLL_API_ID, HttpStatus.BAD_GATEWAY, MSG_ENROLL_FAILED);
                }
                return autoEnrollResponse(batch, MSG_ENROLLED);
            }
        }
        return createBatchAndEnroll(contentId, userUUID, authUserToken);
    }

    private List<CohortUsers> fetchParticipantsList(String authUserToken, String rootOrg, List<String> batchIdList,
            int count) {
        List<String> participantList = courseClient.getParticipants(authUserToken, batchIdList);
        List<CohortUsers> activeUserCollection = new ArrayList<>();
        if (participantList.size() > count) {
            participantList = participantList.stream().limit(count).collect(Collectors.toList());
        } else if (participantList.isEmpty()) {
            return activeUserCollection;
        }
        try {
            Map<String, UserSearchContent> participantMap = userSearchClient.getUsersByIds(participantList);
            if (!participantMap.isEmpty()) {
                int currentCount = 0;
                String desc = "Started learning this course";
                for (String userId : participantList) {
                    UserSearchContent userInfo = participantMap.get(userId);
                    if (userInfo != null) {
                        CohortUsers user = new CohortUsers();
                        user.setUserId(userInfo.getUserId());
                        user.setEmail(userInfo.getEmail());
                        user.setFirstName(userInfo.getFirstName());
                        user.setLastName(userInfo.getLastName());
                        user.setDesc(desc);
                        user.setDepartment(userInfo.getChannel());
                        if (userInfo.getProfileDetails() != null
                                && userInfo.getProfileDetails().getProfessionalDetails() != null
                                && !userInfo.getProfileDetails().getProfessionalDetails().isEmpty()) {
                            ProfessionalDetail profDetail = userInfo.getProfileDetails().getProfessionalDetails().get(0);
                            String designation = profDetail.getDesignation() != null
                                    ? profDetail.getDesignation()
                                    : profDetail.getDesignationOther();
                            user.setDepartment(designation);
                        }
                        activeUserCollection.add(user);
                        currentCount++;
                        if (currentCount == count) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("fetchParticipantsList failed: {}", e.getMessage(), e);
        }
        return activeUserCollection;
    }

    private AppResponse<Map<String, Object>> createBatchAndEnroll(String contentId, String userUUID,
            String authUserToken) {
        String date = courseClient.today();
        String batchId = courseClient.createBatch(contentId, userUUID, authUserToken);
        if (isEmpty(batchId)) {
            throw new ApiException(ENROLL_API_ID, HttpStatus.BAD_GATEWAY, MSG_CREATE_FAILED);
        }
        if (!courseClient.enroll(contentId, userUUID, batchId, authUserToken)) {
            throw new ApiException(ENROLL_API_ID, HttpStatus.BAD_GATEWAY, MSG_ENROLL_FAILED);
        }
        CohortBatch selectedBatch = new CohortBatch();
        selectedBatch.setBatchId(batchId);
        selectedBatch.setEndDate(null);
        selectedBatch.setCreatedFor(new ArrayList<>());
        selectedBatch.setEnrollmentEndDate(null);
        selectedBatch.setEnrollmentType("open");
        selectedBatch.setName("Open Batch");
        selectedBatch.setStartDate(date);
        selectedBatch.setStatus(1);
        return autoEnrollResponse(selectedBatch, MSG_BATCH_CREATED_ENROLLED);
    }

    /** Auto-enrollment result: {@code result.response = {content:[batch], count:1}} plus a message. */
    private AppResponse<Map<String, Object>> autoEnrollResponse(CohortBatch selectedBatch, String message) {
        List<CohortBatch> content = new ArrayList<>();
        content.add(selectedBatch);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(Constants.CONTENT, content);
        response.put(Constants.COUNT, 1);
        return envelope(ENROLL_API_ID, message, response);
    }

    /** Builds the uniform envelope: {@code result = {"message": message, "response": response}}. */
    private AppResponse<Map<String, Object>> envelope(String apiId, String message, Object response) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", message);
        result.put(Constants.RESPONSE, response);
        return AppResponse.success(apiId, result, HttpStatus.OK);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
