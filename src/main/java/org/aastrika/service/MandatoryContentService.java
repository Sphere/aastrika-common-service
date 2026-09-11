package org.aastrika.service;

import java.util.Map;

import org.aastrika.dto.response.AppResponse;
import org.aastrika.dto.response.MandatoryContentResponse;

/**
 * Mandatory-content status check migrated from the source {@code MandatoryContentService}: reads the
 * org's mandatory courses from Cassandra and enriches each with the user's progress from the course
 * service. v1 returns the service's uniform {@link AppResponse} envelope
 * ({@code result = {"message": ..., "response": ...}}); v2 returns the raw {@link MandatoryContentResponse}
 * (documented envelope exception — see CLAUDE.md).
 */
public interface MandatoryContentService {

    AppResponse<Map<String, Object>> getMandatoryContentStatusForUser(String authUserToken, String rootOrg,
            String org, String userId);

    MandatoryContentResponse getMandatoryContentStatus(String authUserToken, String rootOrg, String org,
            String userId);
}
