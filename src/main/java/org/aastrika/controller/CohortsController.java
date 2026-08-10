package org.aastrika.controller;

import java.util.Map;

import org.aastrika.dto.response.AppResponse;
import org.aastrika.service.CohortsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cohort endpoints migrated from the source {@code CohortsController} (path-variable variants only):
 * active-users and auto-enrollment. Both return the service's uniform {@link AppResponse} envelope
 * ({@code result = {"message": ..., "response": ...}}). The {@code Authorization} header's optional
 * "Bearer " prefix is stripped, as in the source.
 */
@RestController
public class CohortsController {

    private final CohortsService cohortsService;

    public CohortsController(CohortsService cohortsService) {
        this.cohortsService = cohortsService;
    }

    /** Active users (participants) across the live batches of a resource. */
    @GetMapping("/v2/resources/{resourceId}/user/{userUUID}/cohorts/activeusers")
    public ResponseEntity<AppResponse<Map<String, Object>>> getActiveUsers(
            @RequestHeader("Authorization") String authUserToken,
            @PathVariable("resourceId") String contentId,
            @RequestHeader("rootOrg") String rootOrg,
            @PathVariable("userUUID") String userUUID,
            @RequestParam(value = "count", required = false, defaultValue = "50") Integer count,
            @RequestParam(value = "filter", required = false, defaultValue = "false") Boolean toFilter) {
        if (authUserToken.contains(" ")) {
            authUserToken = authUserToken.split(" ")[1];
        }
        return new ResponseEntity<>(
                cohortsService.getActiveUsers(authUserToken, rootOrg, contentId, userUUID, count, toFilter),
                HttpStatus.OK);
    }

    /** Auto-enrols the user into the course (reusing an open batch, or creating one). */
    @GetMapping("/v1/autoenrollment/{userUUID}/{courseId}")
    public ResponseEntity<AppResponse<Map<String, Object>>> autoEnrollmentInCourse(
            @RequestHeader("Authorization") String authUserToken,
            @PathVariable("courseId") String contentId,
            @RequestHeader("rootOrg") String rootOrg,
            @PathVariable("userUUID") String userUUID) {
        if (authUserToken.contains(" ")) {
            authUserToken = authUserToken.split(" ")[1];
        }
        return new ResponseEntity<>(
                cohortsService.autoEnrollmentInCourse(authUserToken, rootOrg, contentId, userUUID),
                HttpStatus.OK);
    }
}
