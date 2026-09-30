package org.aastrika.service;

import java.util.Map;

import org.aastrika.dto.response.AppResponse;

/**
 * Cohort operations migrated from the source {@code CohortsService}: active-users (read) plus
 * auto-enrollment (course-service writes). All external calls (user-search, course-service) are
 * preserved as external calls. Every method returns the service's uniform {@link AppResponse}
 * envelope with a {@code result} of {@code {"message": ..., "response": ...}}.
 */
public interface CohortsService {

    /** Active users (participants) across the live batches of a resource. */
    AppResponse<Map<String, Object>> getActiveUsers(String authUserToken, String rootOrg, String contentId,
            String userUUID, int count, boolean toFilter);

    /** Auto-enrols the user into the course (reusing an open batch, or creating one), echoing the batch. */
    AppResponse<Map<String, Object>> autoEnrollmentInCourse(String authUserToken, String rootOrg, String contentId,
            String userUUID);
}
