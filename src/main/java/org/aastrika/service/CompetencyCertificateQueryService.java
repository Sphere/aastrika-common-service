package org.aastrika.service;

import java.util.List;
import java.util.Map;

import org.aastrika.dto.request.CompetencyCertificateRequest;
import org.aastrika.dto.response.AppResponse;

public interface CompetencyCertificateQueryService {

    /** Certificate rows matching the given filters (any combination of rootOrgId, userId, competencyId). */
    AppResponse<List<Map<String, Object>>> getStatus(CompetencyCertificateRequest request);

    /** Generated certificates (with a URL) matching the given filters. */
    AppResponse<List<Map<String, Object>>> download(CompetencyCertificateRequest request);
}