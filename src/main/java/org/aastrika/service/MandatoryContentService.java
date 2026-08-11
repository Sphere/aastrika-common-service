package org.aastrika.service;

import java.util.Map;

import org.aastrika.dto.response.AppResponse;

/**
 * Mandatory-content status check migrated from the source {@code MandatoryContentService}: reads the
 * org's mandatory courses from Cassandra and enriches each with the user's progress from the course
 * service. Returns the service's uniform {@link AppResponse} envelope
 * ({@code result = {"message": ..., "response": ...}}).
 */
public interface MandatoryContentService {

    AppResponse<Map<String, Object>> getMandatoryContentStatusForUser(String authUserToken, String rootOrg,
            String org, String userId);
}
