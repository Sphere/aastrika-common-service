package org.aastrika.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.aastrika.dto.request.CertificateRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.entity.UserProgramCompletion;
import org.aastrika.entity.UserProgramCompletionId;
import org.aastrika.exception.ApiException;
import org.aastrika.repository.UserProgramCompletionRepository;
import org.aastrika.service.CertificateService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CertificateServiceImpl implements CertificateService {

    private static final String API_ID = "api.program.certification";

    private final UserProgramCompletionRepository userProgramCompletionRepository;

    public CertificateServiceImpl(UserProgramCompletionRepository userProgramCompletionRepository) {
        this.userProgramCompletionRepository = userProgramCompletionRepository;
    }

    @Override
    public AppResponse<Map<String, Object>> getStatus(CertificateRequest certificateRequest) {
        UserProgramCompletion userProgramCompletion = findOrThrow(certificateRequest);
        Map<String, Object> result = baseResult(certificateRequest);
        result.put("isCompleted", Boolean.TRUE.equals(userProgramCompletion.getCompleted()));
        result.put("isCertificateGenerated", Boolean.TRUE.equals(userProgramCompletion.getCertificateGenerated()));
        return AppResponse.success(API_ID, result, HttpStatus.OK);
    }

    @Override
    public AppResponse<Map<String, Object>> download(CertificateRequest certificateRequest) {
        UserProgramCompletion userProgramCompletion = findOrThrow(certificateRequest);

        if (!Boolean.TRUE.equals(userProgramCompletion.getCompleted())) {
            throw new ApiException(API_ID, HttpStatus.BAD_REQUEST, "PROGRAM_NOT_COMPLETED");
        }

        String certificateUrl = userProgramCompletion.getCertificateUrl();
        if (certificateUrl == null || certificateUrl.isBlank()) {
            throw new ApiException(API_ID, HttpStatus.NOT_FOUND, "CERTIFICATE_NOT_GENERATED");
        }

        Map<String, Object> result = baseResult(certificateRequest);
        result.put("certificateUrl", certificateUrl);
        return AppResponse.success(API_ID, result, HttpStatus.OK);
    }

    private UserProgramCompletion findOrThrow(CertificateRequest certificateRequest) {
        UserProgramCompletionId userProgramCompletionId = new UserProgramCompletionId(
                certificateRequest.getRootOrgId(), certificateRequest.getProgramId(), certificateRequest.getUserId());
        return userProgramCompletionRepository.findById(userProgramCompletionId)
                .orElseThrow(() -> new ApiException(API_ID, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND"));
    }

    private static Map<String, Object> baseResult(CertificateRequest certificateRequest) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rootOrgId", certificateRequest.getRootOrgId());
        result.put("userId", certificateRequest.getUserId());
        result.put("programId", certificateRequest.getProgramId());
        return result;
    }
}
