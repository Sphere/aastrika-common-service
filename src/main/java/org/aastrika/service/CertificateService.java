package org.aastrika.service;

import java.util.Map;

import org.aastrika.dto.request.CertificateRequest;
import org.aastrika.dto.response.AppResponse;

public interface CertificateService {

    /**
     * Certificate/completion status for the given (rootOrg, program, user).
     *
     * @throws org.aastrika.exception.ApiException 404 RESOURCE_NOT_FOUND if no completion row exists
     */
    AppResponse<Map<String, Object>> getStatus(CertificateRequest request);

    /**
     * Resolves the downloadable certificate URL for the given (rootOrg, program, user).
     *
     * @throws org.aastrika.exception.ApiException 404 RESOURCE_NOT_FOUND if no completion row exists;
     *                                             400 PROGRAM_NOT_COMPLETED if not completed;
     *                                             404 CERTIFICATE_NOT_GENERATED if no certificate URL
     */
    AppResponse<Map<String, Object>> download(CertificateRequest request);
}
