package org.aastrika.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aastrika.dto.request.CompetencyCertificateRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.entity.UserCompetencyCertificate;
import org.aastrika.exception.ApiException;
import org.aastrika.repository.UserCompetencyCertificateRepository;
import org.aastrika.service.CompetencyCertificateQueryService;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Reads {@code user_competency_certificate} by any combination of rootOrgId, userId and
 * competencyId (query-by-example: null probe fields are ignored). No match is an empty list, not 404.
 */
@Service
public class CompetencyCertificateQueryServiceImpl implements CompetencyCertificateQueryService {

    private static final String API_ID = "api.competency.certification";
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "updatedAt");

    private final UserCompetencyCertificateRepository certificateRepository;

    public CompetencyCertificateQueryServiceImpl(UserCompetencyCertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    @Override
    public AppResponse<List<Map<String, Object>>> getStatus(CompetencyCertificateRequest request) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserCompetencyCertificate row : find(request)) {
            Map<String, Object> item = baseResult(row);
            item.put("status", row.getStatus());
            item.put("isCertificateGenerated", UserCompetencyCertificate.GENERATED.equals(row.getStatus()));
            item.put("message", row.getMessage());
            item.put("completedAt", row.getCompletedAt());
            item.put("updatedAt", row.getUpdatedAt());
            result.add(item);
        }
        return AppResponse.success(API_ID, result, HttpStatus.OK);
    }

    @Override
    public AppResponse<List<Map<String, Object>>> download(CompetencyCertificateRequest request) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserCompetencyCertificate row : find(request)) {
            if (UserCompetencyCertificate.GENERATED.equals(row.getStatus())
                    && row.getCertificateUrl() != null && !row.getCertificateUrl().isBlank()) {
                Map<String, Object> item = baseResult(row);
                item.put("certificateUrl", row.getCertificateUrl());
                result.add(item);
            }
        }
        return AppResponse.success(API_ID, result, HttpStatus.OK);
    }

    private List<UserCompetencyCertificate> find(CompetencyCertificateRequest request) {
        CompetencyCertificateRequest.Payload payload = request.getRequest();
        CompetencyCertificateRequest.Filters filters = payload.getFilters();
        UserCompetencyCertificate probe = new UserCompetencyCertificate();
        if (filters != null) {
            probe.setRootOrgId(blankToNull(filters.getRootOrgId()));
            probe.setUserId(blankToNull(filters.getUserId()));
            probe.setCompetencyId(blankToNull(filters.getCompetencyId()));
        }
        if (probe.getRootOrgId() == null && probe.getUserId() == null && probe.getCompetencyId() == null) {
            throw new ApiException(API_ID, HttpStatus.BAD_REQUEST,
                    "At least one of filters.rootOrgId, filters.userId, filters.competencyId is required");
        }
        return certificateRepository.findAll(Example.of(probe), NEWEST_FIRST);
    }

    private static Map<String, Object> baseResult(UserCompetencyCertificate row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("rootOrgId", row.getRootOrgId());
        item.put("userId", row.getUserId());
        item.put("competencyId", row.getCompetencyId());
        item.put("competencyName", row.getCompetencyName());
        return item;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}