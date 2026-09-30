package org.aastrika.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aastrika.common.Constants;
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
 * HTTP client for the learner service's user-administration calls behind
 * {@code PATCH /user/v1/migrate}: the org migration itself, the follow-up PUBLIC role assignment and
 * the data-sync (search reindex) trigger. Faithful port of the source
 * {@code ProfileServiceImpl.executeMigrateUser}/{@code syncUserData} and
 * {@code UserUtilityServiceImpl.assignRole} — same paths, bodies and headers.
 *
 * <p>Each method returns {@code null} on success and a non-null message on failure, mirroring the
 * source's {@code errMsg} convention so the service can surface the downstream reason verbatim.
 * Messages are taken from the downstream {@code params.errmsg}; nothing user-identifying is logged
 * here (SECURITY.md 2.5).
 */
@Component
@Slf4j
public class UserMigrationClient {

    private static final String OK = "OK";

    private final RestTemplate restTemplate;
    private final String serviceUrl;
    private final String migratePath;
    private final String assignRolePath;
    private final String dataSyncPath;

    public UserMigrationClient(
            RestTemplate contentRestTemplate,
            @Value("${learn-service.url:http://localhost:9000}") String serviceUrl,
            @Value("${learn-service.user.migrate-path:/private/user/v1/migrate}") String migratePath,
            @Value("${learn-service.user.assign-role-path:/private/user/v1/assign/role}") String assignRolePath,
            @Value("${learn-service.data-sync-path:/v1/data/sync}") String dataSyncPath) {
        this.restTemplate = contentRestTemplate;
        this.serviceUrl = serviceUrl;
        this.migratePath = migratePath;
        this.assignRolePath = assignRolePath;
        this.dataSyncPath = dataSyncPath;
    }

    /**
     * Migrates the user to {@code channel}'s organisation. Sends {@code softDeleteOldOrg=true},
     * {@code notifyMigration=false} and {@code forceMigration=true}, as the source does for the admin
     * (non-self) path. This call is <b>not reversible</b> — the old org association is soft-deleted.
     */
    public String migrateUser(String userId, String channel, String userToken, String authToken) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put(Constants.CHANNEL, channel);
        payload.put("softDeleteOldOrg", true);
        payload.put("notifyMigration", false);
        payload.put("forceMigration", true);

        HttpHeaders headers = jsonHeaders();
        headers.set(Constants.X_AUTH_TOKEN, userToken);
        if (authToken != null && !authToken.isBlank()) {
            headers.set(Constants.AUTH_TOKEN, authToken);
        }

        return execute(HttpMethod.PATCH, serviceUrl + migratePath, wrap(payload), headers,
                "Failed to migrate User.");
    }

    /** Assigns the PUBLIC role to the user within {@code organisationId}. */
    public String assignPublicRole(String organisationId, String userId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("organisationId", organisationId);
        payload.put("userId", userId);
        payload.put("roles", List.of("PUBLIC"));

        return execute(HttpMethod.POST, serviceUrl + assignRolePath, wrap(payload), jsonHeaders(),
                "Failed to assign PUBLIC role to user.");
    }

    /** Triggers a user data-sync so the search index reflects the new org. */
    public String syncUser(String userId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operationType", "sync");
        payload.put("objectIds", List.of(userId));
        payload.put("objectType", "user");

        return execute(HttpMethod.POST, serviceUrl + dataSyncPath, wrap(payload), jsonHeaders(),
                "Failed to call Data Sync after updating Profile.");
    }

    /** Returns null on success, otherwise the downstream {@code params.errmsg} or {@code fallback}. */
    @SuppressWarnings("unchecked")
    private String execute(HttpMethod method, String url, Map<String, Object> body, HttpHeaders headers,
                           String fallback) {
        Map<String, Object> response;
        try {
            response = restTemplate.exchange(url, method, new HttpEntity<>(body, headers), Map.class).getBody();
        } catch (RestClientException e) {
            // No user identifiers in the log line (SECURITY.md 2.5).
            log.error("{} {} failed: {}", method, url, e.getMessage());
            return fallback;
        }
        if (response == null) {
            return fallback;
        }
        if (OK.equalsIgnoreCase((String) response.get("responseCode"))) {
            return null;
        }
        Object params = response.get("params");
        if (params instanceof Map<?, ?> paramMap) {
            Object errmsg = ((Map<String, Object>) paramMap).get("errmsg");
            if (errmsg != null && !errmsg.toString().isBlank()) {
                return errmsg.toString();
            }
        }
        return fallback;
    }

    private static Map<String, Object> wrap(Map<String, Object> payload) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put(Constants.REQUEST, payload);
        return request;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
