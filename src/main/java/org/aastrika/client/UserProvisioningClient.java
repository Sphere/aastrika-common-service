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
 * Learner-service calls needed to provision one user from a bulk-upload row. Faithful port of the
 * source {@code UserUtilityServiceImpl}: same endpoints, bodies and headers.
 *
 * <p>Provisioning a single user is a four-call chain — create, read back, patch the profile, assign
 * the PUBLIC role — and the source treats the user as provisioned only if all four succeed. That is
 * preserved: a user created but left without a profile or role would look successful while being
 * unusable.
 *
 * <p>Two source defects are fixed here:
 * <ul>
 *   <li><b>URL joining.</b> The source concatenated {@code sbUrl + "private/user/v1/search"} with no
 *       separator, producing {@code http://host:9000private/user/v1/search}. Paths are joined with
 *       exactly one slash regardless of how they are configured.</li>
 *   <li><b>Unguarded response maps.</b> The source dereferenced {@code readData.get(...)} without
 *       null checks, so a non-JSON or empty response threw NPE mid-row.</li>
 * </ul>
 *
 * <p>No user identifiers are logged (SECURITY.md 2.5).
 */
@Component
@Slf4j
public class UserProvisioningClient {

    private static final String OK = "OK";

    private final RestTemplate restTemplate;
    private final String serviceUrl;
    private final String createPath;
    private final String readPath;
    private final String updatePath;
    private final String assignRolePath;
    private final String searchPath;
    private final String apiKey;

    public UserProvisioningClient(
            RestTemplate contentRestTemplate,
            @Value("${learn-service.url:http://localhost:9000}") String serviceUrl,
            @Value("${learn-service.user.create-path:/v3/user/create}") String createPath,
            @Value("${learn-service.user.read-path:/private/user/v1/read/}") String readPath,
            @Value("${learn-service.user.update-path:/private/user/v1/update}") String updatePath,
            @Value("${learn-service.user.assign-role-path:/private/user/v1/assign/role}") String assignRolePath,
            @Value("${learn-service.user.search-path:/private/user/v1/search}") String searchPath,
            @Value("${sb.api-key:apiKey}") String apiKey) {
        this.restTemplate = contentRestTemplate;
        this.serviceUrl = serviceUrl;
        this.createPath = createPath;
        this.readPath = readPath;
        this.updatePath = updatePath;
        this.assignRolePath = assignRolePath;
        this.searchPath = searchPath;
        this.apiKey = apiKey;
    }

    /**
     * True if a user already exists with {@code field} = {@code value} ({@code email} or {@code phone}).
     *
     * <p>Returns {@code true} when the response cannot be interpreted, matching the source: an
     * unreadable answer must not be taken as licence to create a possibly-duplicate account.
     */
    @SuppressWarnings("unchecked")
    public boolean exists(String field, String value) {
        Map<String, Object> body = wrap(Map.of(Constants.FILTERS, Map.of(field, value)));
        HttpHeaders headers = jsonHeaders();
        headers.set(Constants.AUTH_TOKEN, apiKey);
        try {
            Map<String, Object> response = restTemplate
                    .exchange(join(searchPath), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class).getBody();
            if (response == null || !OK.equalsIgnoreCase((String) response.get("responseCode"))) {
                return true;
            }
            Object result = response.get("result");
            if (!(result instanceof Map<?, ?> resultMap)) {
                return true;
            }
            Object inner = ((Map<String, Object>) resultMap).get(Constants.RESPONSE);
            if (!(inner instanceof Map<?, ?> responseMap)) {
                return true;
            }
            Object count = ((Map<String, Object>) responseMap).get(Constants.COUNT);
            return !(count instanceof Number number) || number.intValue() != 0;
        } catch (RestClientException e) {
            log.error("User search failed on {}: {}", field, e.getMessage());
            return true;
        }
    }

    /**
     * Runs the full provisioning chain for one row.
     *
     * @return {@code null} on success, otherwise a message describing which step failed
     */
    @SuppressWarnings("unchecked")
    public String provision(String firstName, String lastName, String email, String phone, String orgName) {
        // 1) create
        Map<String, Object> createBody = new LinkedHashMap<>();
        createBody.put("email", email);
        createBody.put(Constants.CHANNEL, orgName);
        createBody.put("firstName", firstName);
        createBody.put("lastName", lastName);
        createBody.put("emailVerified", true);
        createBody.put("phone", phone);
        createBody.put("phoneVerified", true);

        Map<String, Object> created = post(join(createPath), wrap(createBody), jsonHeaders());
        if (created == null || !OK.equalsIgnoreCase((String) created.get("responseCode"))) {
            return errorOf(created, "Failed to create user");
        }
        String userId = null;
        if (created.get("result") instanceof Map<?, ?> result) {
            userId = (String) ((Map<String, Object>) result).get("userId");
        }
        if (userId == null || userId.isBlank()) {
            return "User was created but no userId was returned";
        }

        // 2) read back — needed for rootOrgId, which the role assignment is scoped to
        Map<String, Object> user = readUser(userId);
        if (user == null) {
            return "User was created but could not be read back";
        }
        String rootOrgId = (String) user.get("rootOrgId");

        // 3) patch the profile
        String updateError = updateProfile(userId, firstName, lastName, email, phone, orgName);
        if (updateError != null) {
            return updateError;
        }

        // 4) PUBLIC role, scoped to the org the user landed in
        Map<String, Object> roleBody = new LinkedHashMap<>();
        roleBody.put("organisationId", rootOrgId);
        roleBody.put("userId", userId);
        roleBody.put("roles", List.of("PUBLIC"));
        Map<String, Object> role = post(join(assignRolePath), wrap(roleBody), jsonHeaders());
        if (role == null || !OK.equalsIgnoreCase((String) role.get("responseCode"))) {
            return errorOf(role, "User was created but the PUBLIC role could not be assigned");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readUser(String userId) {
        try {
            Map<String, Object> response = restTemplate.exchange(join(readPath) + userId, HttpMethod.GET,
                    new HttpEntity<>(jsonHeaders()), Map.class).getBody();
            if (response == null || !(response.get("result") instanceof Map<?, ?> result)) {
                return null;
            }
            Object inner = ((Map<String, Object>) result).get(Constants.RESPONSE);
            return inner instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
        } catch (RestClientException e) {
            log.error("User read-back failed: {}", e.getMessage());
            return null;
        }
    }

    private String updateProfile(String userId, String firstName, String lastName, String email, String phone,
                                 String orgName) {
        Map<String, Object> personal = new LinkedHashMap<>();
        personal.put("firstname", firstName);
        personal.put("surname", lastName);
        personal.put("primaryEmail", email);
        personal.put("mobile", phone);
        personal.put("phoneVerified", true);

        Map<String, Object> professional = new LinkedHashMap<>();
        professional.put("organizationType", "Government");

        Map<String, Object> profileDetails = new LinkedHashMap<>();
        profileDetails.put("mandatoryFieldsExists", false);
        profileDetails.put("employmentDetails", Map.of("departmentName", orgName));
        profileDetails.put("personalDetails", personal);
        profileDetails.put("professionalDetails", List.of(professional));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        body.put("profileDetails", profileDetails);

        try {
            Map<String, Object> response = restTemplate.exchange(join(updatePath), HttpMethod.PATCH,
                    new HttpEntity<>(wrap(body), jsonHeaders()), Map.class).getBody();
            if (response == null || !OK.equalsIgnoreCase((String) response.get("responseCode"))) {
                return errorOf(response, "User was created but the profile update failed");
            }
            return null;
        } catch (RestClientException e) {
            log.error("Profile update failed: {}", e.getMessage());
            return "User was created but the profile update failed";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String url, Map<String, Object> body, HttpHeaders headers) {
        try {
            return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class).getBody();
        } catch (RestClientException e) {
            log.error("POST {} failed: {}", url, e.getMessage());
            return null;
        }
    }

    /** Downstream {@code params.errmsg} when present, else {@code fallback}. */
    @SuppressWarnings("unchecked")
    private static String errorOf(Map<String, Object> response, String fallback) {
        if (response != null && response.get("params") instanceof Map<?, ?> params) {
            Object errmsg = ((Map<String, Object>) params).get("errmsg");
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

    /** Joins base and path with exactly one slash, however either is configured. */
    private String join(String path) {
        String base = serviceUrl.endsWith("/") ? serviceUrl.substring(0, serviceUrl.length() - 1) : serviceUrl;
        return path.startsWith("/") ? base + path : base + "/" + path;
    }
}
