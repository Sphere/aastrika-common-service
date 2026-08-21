package org.aastrika.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * HTTP client for the course (LMS) service. Used by the cohort endpoints (batch participants, a
 * user's enrolled batches, and — for auto-enrollment — creating an open batch and enrolling the user)
 * and by the mandatory-content status check (a user's course progress). Faithful port of the source
 * {@code ContentServiceImpl}/{@code CohortsServiceImpl}/{@code MandatoryContentServiceImpl} course
 * calls: same endpoints, headers ({@code x-authenticated-user-token} + the service api key) and
 * request bodies. These stay external calls by design.
 */
@Component
@Slf4j
public class CourseClient {

    private final RestTemplate restTemplate;
    private final String serviceHost;
    private final String participantsEndpoint;
    private final String userCoursesList;
    private final String batchCreateEndpoint;
    private final String enrollEndpoint;
    private final String progressEndpoint;
    private final String apiKey;

    public CourseClient(
            RestTemplate contentRestTemplate,
            @Value("${cohorts.course.service-host:http://localhost:9000/}") String serviceHost,
            @Value("${cohorts.course.participants-endpoint:v1/batch/participants/list}") String participantsEndpoint,
            @Value("${cohorts.course.user-courses-list:v1/user/courses/list/{userUUID}}") String userCoursesList,
            @Value("${cohorts.course.batch-create-endpoint:v1/course/batch/create}") String batchCreateEndpoint,
            @Value("${cohorts.course.enroll-endpoint:v1/course/enroll}") String enrollEndpoint,
            @Value("${learn-service.course.progress-endpoint:http://localhost:9000/private/v1/content/state/read}") String progressEndpoint,
            @Value("${sb.api-key:apiKey}") String apiKey) {
        this.restTemplate = contentRestTemplate;
        this.serviceHost = serviceHost;
        this.participantsEndpoint = participantsEndpoint;
        this.userCoursesList = userCoursesList;
        this.batchCreateEndpoint = batchCreateEndpoint;
        this.enrollEndpoint = enrollEndpoint;
        this.progressEndpoint = progressEndpoint;
        this.apiKey = apiKey;
    }

    /**
     * Participant user-ids across the given batches. One POST per batch (as in the source); batches
     * with a non-"OK" response or a zero count are skipped. Returns an empty list for null/empty input.
     */
    @SuppressWarnings("unchecked")
    public List<String> getParticipants(String authUserToken, List<String> batchIdList) {
        List<String> participantList = new ArrayList<>();
        if (batchIdList == null || batchIdList.isEmpty()) {
            return participantList;
        }
        String url = serviceHost + participantsEndpoint;
        HttpHeaders headers = authHeaders(authUserToken);
        for (String batchId : batchIdList) {
            try {
                Map<String, Object> batch = new LinkedHashMap<>();
                batch.put("active", true);
                batch.put("batchId", batchId);
                Map<String, Object> requestBody = Map.of("request", Map.of("batch", batch));

                Map<String, Object> resp = restTemplate.postForObject(
                        url, new HttpEntity<>(requestBody, headers), Map.class);
                if (resp != null && "OK".equalsIgnoreCase(String.valueOf(resp.get("responseCode")))) {
                    Map<String, Object> result = (Map<String, Object>) resp.get("result");
                    Map<String, Object> batchResp = result == null ? null : (Map<String, Object>) result.get("batch");
                    if (batchResp != null && ((Number) batchResp.getOrDefault("count", 0)).intValue() > 0) {
                        List<String> participants = (List<String>) batchResp.get("participants");
                        if (participants != null) {
                            participantList.addAll(participants);
                        }
                    }
                    log.info("Fetch Participants return - {} no. of users.", participantList.size());
                } else {
                    log.warn("Failed to get participants for BatchId - {}", batchId);
                }
            } catch (RestClientException e) {
                log.error("getParticipants failed for batch {}: {}", batchId, e.getMessage());
            }
        }
        return participantList;
    }

    /** The batch-ids a user is currently enrolled in ({@code result.courses[].batchId}); empty on miss. */
    @SuppressWarnings("unchecked")
    public List<String> getUserEnrolledBatchIds(String authUserToken, String userId) {
        List<String> batchIds = new ArrayList<>();
        String url = serviceHost + userCoursesList.replace("{userUUID}", userId);
        try {
            Map<String, Object> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeader(authUserToken)), Map.class).getBody();
            if (resp != null && "OK".equalsIgnoreCase(String.valueOf(resp.get("responseCode")))) {
                Map<String, Object> result = (Map<String, Object>) resp.get("result");
                List<Map<String, Object>> courses = result == null ? null
                        : (List<Map<String, Object>>) result.get("courses");
                if (courses != null) {
                    for (Map<String, Object> course : courses) {
                        Object batchId = course.get("batchId");
                        if (batchId != null) {
                            batchIds.add(batchId.toString());
                        }
                    }
                }
            }
        } catch (RestClientException e) {
            log.error("getUserEnrolledBatchIds failed for {}: {}", userId, e.getMessage());
        }
        return batchIds;
    }

    /**
     * Creates an "Open Batch" for the course. Returns the new batch-id, or {@code null} if the create
     * failed (transport error, or a response that carried no batch-id) — the caller treats null as a
     * write failure. {@code startDate} is today (yyyy-MM-dd), as in the source.
     */
    @SuppressWarnings("unchecked")
    public String createBatch(String contentId, String userUUID, String authUserToken) {
        String date = LocalDate.now().toString();
        Map<String, Object> batchObj = new LinkedHashMap<>();
        batchObj.put("courseId", contentId);
        batchObj.put("name", "Open Batch");
        batchObj.put("description", "Open Batch");
        batchObj.put("enrollmentType", "open");
        batchObj.put("startDate", date);
        batchObj.put("createdBy", userUUID);
        Map<String, Object> requestBody = Map.of("request", batchObj);

        try {
            Map<String, Object> resp = restTemplate.postForObject(
                    serviceHost + batchCreateEndpoint, new HttpEntity<>(requestBody, authHeaders(authUserToken)),
                    Map.class);
            Map<String, Object> result = resp == null ? null : (Map<String, Object>) resp.get("result");
            return result == null ? null : (String) result.get("batchId");
        } catch (RestClientException e) {
            log.error("createBatch failed for course {}: {}", contentId, e.getMessage());
            return null;
        }
    }

    /** Enrols a user into a course batch. Returns true on success, false on a transport failure. */
    public boolean enroll(String contentId, String userUUID, String batchId, String authUserToken) {
        Map<String, Object> enrollObj = new LinkedHashMap<>();
        enrollObj.put("userId", userUUID);
        enrollObj.put("courseId", contentId);
        enrollObj.put("batchId", batchId);
        Map<String, Object> requestBody = Map.of("request", enrollObj);

        try {
            restTemplate.postForObject(
                    serviceHost + enrollEndpoint, new HttpEntity<>(requestBody, authHeaders(authUserToken)), Map.class);
            return true;
        } catch (RestClientException e) {
            log.error("enroll failed for course {} batch {}: {}", contentId, batchId, e.getMessage());
            return false;
        }
    }

    /**
     * A user's completion percentage for a course/batch, via the progress read
     * ({@code result.contentList[0].completionPercentage}). Faithful port of the source
     * {@code MandatoryContentServiceImpl.enrichProgressDetails} call. Returns {@code null} on a
     * non-OK response, an empty content list, or a transport/parse error.
     */
    @SuppressWarnings("unchecked")
    public Float getCourseProgress(String authUserToken, String userId, String courseId, String batchId) {
        Map<String, Object> reqObj = new LinkedHashMap<>();
        reqObj.put("userId", userId);
        reqObj.put("courseId", courseId);
        reqObj.put("batchId", batchId);
        reqObj.put("fields", List.of("progressdetails"));
        Map<String, Object> requestBody = Map.of("request", reqObj);
        try {
            // progressEndpoint is a FULL absolute URL (unlike the other course paths) — used as-is,
            // NOT prefixed with serviceHost.
            Map<String, Object> resp = restTemplate.postForObject(
                    progressEndpoint, new HttpEntity<>(requestBody, authHeaders(authUserToken)),
                    Map.class);
            if (resp == null || !"OK".equalsIgnoreCase(String.valueOf(resp.get("responseCode")))) {
                return null;
            }
            Map<String, Object> result = (Map<String, Object>) resp.get("result");
            List<Map<String, Object>> contentList = result == null ? null
                    : (List<Map<String, Object>>) result.get("contentList");
            if (contentList == null || contentList.isEmpty()) {
                return null;
            }
            Object pct = contentList.get(0).get("completionPercentage");
            return pct == null ? null : new BigDecimal(pct.toString()).floatValue();
        } catch (RestClientException | NumberFormatException e) {
            log.warn("getCourseProgress failed for user {} course {}: {}", userId, courseId, e.getMessage());
            return null;
        }
    }

    /** Today's date as {@code yyyy-MM-dd} (exposed so the service can build the echoed batch identically). */
    public String today() {
        return LocalDate.now().toString();
    }

    private HttpHeaders authHeaders(String authUserToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-authenticated-user-token", authUserToken);
        headers.set("Authorization", apiKey);
        return headers;
    }

    private HttpHeaders authHeader(String authUserToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-authenticated-user-token", authUserToken);
        return headers;
    }
}
