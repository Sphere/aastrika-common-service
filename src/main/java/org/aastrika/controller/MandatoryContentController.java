package org.aastrika.controller;

import java.util.Map;

import org.aastrika.dto.response.AppResponse;
import org.aastrika.service.MandatoryContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mandatory-content status endpoint migrated from the source {@code MandatoryContentController}.
 * Returns the service's uniform {@link AppResponse} envelope. Header names match the source contract:
 * {@code xAuthUser} (course-service token), {@code rootOrg}, {@code org}, {@code wid} (the user id).
 */
@RestController
public class MandatoryContentController {

    private final MandatoryContentService mandatoryContentService;

    public MandatoryContentController(MandatoryContentService mandatoryContentService) {
        this.mandatoryContentService = mandatoryContentService;
    }

    /** Whether the user has completed every mandatory course configured for their org. */
    @GetMapping("/v1/check/mandatoryContentStatus")
    public ResponseEntity<AppResponse<Map<String, Object>>> getMandatoryContentStatus(
            @RequestHeader("xAuthUser") String authUserToken,
            @RequestHeader("rootOrg") String rootOrg,
            @RequestHeader("org") String org,
            @RequestHeader("wid") String userId) {
        return ResponseEntity.ok(
                mandatoryContentService.getMandatoryContentStatusForUser(authUserToken, rootOrg, org, userId));
    }
}
