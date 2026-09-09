package org.aastrika.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aastrika.common.Constants;
import org.aastrika.dto.response.UserSearchContent;
import org.aastrika.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Validates a user by calling the external user-search service — a faithful port of the source's
 * {@code UserUtilityService.validateUser} (POST {@code {sb.service.url}/private/user/v1/search} with
 * a {@code request.filters.userId} body; valid when {@code responseCode=OK} and
 * {@code result.response.count >= 1}). This stays an external call by design — it is not replaced
 * with a local DB lookup.
 */
@Component
public class UserSearchClient {

    private static final String API_ID = "api.assessment.submit";
    private static final String COHORT_API_ID = "api.cohorts.read";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final String userSearchUrl;

    public UserSearchClient(
            RestTemplate contentRestTemplate,
            @Value("${user.search-url:http://localhost:8080/private/user/v1/search}") String userSearchUrl) {
        this.restTemplate = contentRestTemplate;
        this.userSearchUrl = userSearchUrl;
    }

    /** {@code rootOrg} is kept for signature parity with the source; only {@code userId} is filtered. */
    @SuppressWarnings("unchecked")
    public boolean validateUser(String rootOrg, String userId) {
        Map<String, Object> body = Map.of(Constants.REQUEST, Map.of(Constants.FILTERS, Map.of("userId", userId)));
        try {
            Map<String, Object> resp = restTemplate.postForObject(
                    userSearchUrl, new HttpEntity<>(body, jsonHeaders()), Map.class);
            if (resp == null || !"OK".equalsIgnoreCase(String.valueOf(resp.get("responseCode")))) {
                return false;
            }
            Map<String, Object> result = (Map<String, Object>) resp.get("result");
            Map<String, Object> response = result == null ? null : (Map<String, Object>) result.get(Constants.RESPONSE);
            Object count = response == null ? null : response.get(Constants.COUNT);
            return count instanceof Number && ((Number) count).intValue() >= 1;
        } catch (RestClientException e) {
            throw new ApiException(API_ID, HttpStatus.INTERNAL_SERVER_ERROR,
                    "User service error: " + e.getMessage());
        }
    }

    /**
     * User details for a set of ids, keyed by {@code userId} — a faithful port of the source
     * {@code getUsersDataFromUserIds} (POST {@code {request.filters.userId: [ids]}}, reading
     * {@code result.response.content[]}). The source's {@code fields}/{@code source} argument is
     * ignored on the wire, so it is omitted here too. Returns an empty map when there are no ids or
     * the search returns nothing; throws {@link ApiException} on a transport error.
     */
    @SuppressWarnings("unchecked")
    public Map<String, UserSearchContent> getUsersByIds(List<String> userIds) {
        Map<String, UserSearchContent> result = new LinkedHashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        Map<String, Object> body = Map.of(Constants.REQUEST, Map.of(Constants.FILTERS, Map.of("userId", userIds)));
        try {
            Map<String, Object> resp = restTemplate.postForObject(
                    userSearchUrl, new HttpEntity<>(body, jsonHeaders()), Map.class);
            if (resp == null || !"OK".equalsIgnoreCase(String.valueOf(resp.get("responseCode")))) {
                return result;
            }
            Map<String, Object> res = (Map<String, Object>) resp.get("result");
            Map<String, Object> response = res == null ? null : (Map<String, Object>) res.get(Constants.RESPONSE);
            if (response == null || ((Number) response.getOrDefault(Constants.COUNT, 0)).intValue() <= 0) {
                return result;
            }
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get(Constants.CONTENT);
            if (content == null) {
                return result;
            }
            for (Map<String, Object> entry : content) {
                UserSearchContent user = MAPPER.convertValue(entry, UserSearchContent.class);
                if (user.getUserId() != null) {
                    result.put(user.getUserId(), user);
                }
            }
            return result;
        } catch (RestClientException | IllegalArgumentException e) {
            throw new ApiException(COHORT_API_ID, HttpStatus.INTERNAL_SERVER_ERROR,
                    "User service error: " + e.getMessage());
        }
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
