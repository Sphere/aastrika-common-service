package org.aastrika.service;

import java.util.Map;

import org.aastrika.dto.request.UserMigrateRequest;
import org.aastrika.dto.response.AppResponse;

public interface ProfileService {

    /**
     * Migrates a user to another organisation: learner-service migration, then a rewrite of the
     * user's {@code profiledetails} to name the new org, then the PUBLIC role assignment and a
     * data-sync. Returns the service's uniform {@link AppResponse} envelope.
     *
     * <p>Not atomic — the learner-service migration is irreversible once it succeeds, so a later
     * failure leaves the user partially migrated. The error result names the step that failed.
     */
    AppResponse<Map<String, Object>> migrateUser(UserMigrateRequest request, String userToken, String authToken);

    AppResponse<Map<String, Object>> userAutoComplete(String searchTerm);
}
