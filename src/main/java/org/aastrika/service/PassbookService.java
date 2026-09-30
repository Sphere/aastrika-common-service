package org.aastrika.service;

import java.util.Map;

import org.aastrika.dto.request.AdminPassbookReadRequest;
import org.aastrika.dto.request.PassbookReadRequest;
import org.aastrika.dto.request.PassbookUpdateRequest;
import org.aastrika.dto.response.AppResponse;

public interface PassbookService {

    /**
     * Adds passbook entries for {@code request.userId}. The {@code actingUserId} is stamped as
     * {@code createdBy} on each entry.
     *
     * @param actingUserId acting user (from the {@code x-authenticated-userid} header)
     * @throws org.aastrika.exception.ApiException 400 if {@code typeName} is unsupported, a
     *                                             competency is listed twice, or an equivalent
     *                                             entry (same course + level) already exists
     */
    AppResponse<Map<String, Object>> updatePassbook(String actingUserId, PassbookUpdateRequest request);

    /**
     * The authenticated user's own passbook for the requested {@code typeName}.
     *
     * @param userId  acting user (from the {@code x-authenticated-userid} header)
     * @throws org.aastrika.exception.ApiException 400 if {@code typeName} is not supported
     */
    AppResponse<Map<String, Object>> getPassbook(String userId, PassbookReadRequest request);

    /**
     * Passbooks of the users named in the request (admin view); the acting user is ignored.
     *
     * @throws org.aastrika.exception.ApiException 400 if {@code typeName} is not supported
     */
    AppResponse<Map<String, Object>> getPassbookByAdmin(AdminPassbookReadRequest request);
}
