package org.aastrika.client;

import java.util.Map;

import org.aastrika.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
        Map<String, Object> body = Map.of("request", Map.of("filters", Map.of("userId", userId)));
        try {
            Map<String, Object> resp = restTemplate.postForObject(
                    userSearchUrl, new HttpEntity<>(body, jsonHeaders()), Map.class);
            if (resp == null || !"OK".equalsIgnoreCase(String.valueOf(resp.get("responseCode")))) {
                return false;
            }
            Map<String, Object> result = (Map<String, Object>) resp.get("result");
            Map<String, Object> response = result == null ? null : (Map<String, Object>) result.get("response");
            Object count = response == null ? null : response.get("count");
            return count instanceof Number && ((Number) count).intValue() >= 1;
        } catch (RestClientException e) {
            throw new ApiException(API_ID, HttpStatus.INTERNAL_SERVER_ERROR,
                    "User service error: " + e.getMessage());
        }
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
