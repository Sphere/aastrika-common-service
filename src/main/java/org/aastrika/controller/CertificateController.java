package org.aastrika.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.aastrika.dto.request.CertificateRequest;
import org.aastrika.dto.request.CompetencyCertificateRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.service.CertificateService;
import org.aastrika.service.CompetencyCertificateQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Program and competency certificate status/download.
 *
 * <p>The un-prefixed {@code /status} and {@code /download} are the original program endpoints, kept
 * as aliases of {@code /program/*} because the portal already calls them. Remove them once the
 * portal has switched to {@code /program/*}.
 */
@RestController
@RequestMapping("/v1/certificate")
public class CertificateController {

    private final CertificateService certificateService;
    private final CompetencyCertificateQueryService competencyCertificateQueryService;

    public CertificateController(CertificateService certificateService,
                                 CompetencyCertificateQueryService competencyCertificateQueryService) {
        this.certificateService = certificateService;
        this.competencyCertificateQueryService = competencyCertificateQueryService;
    }

    @PostMapping({"/program/status", "/status"})
    public ResponseEntity<AppResponse<Map<String, Object>>> status(
            @Valid @RequestBody CertificateRequest certificateRequest) {
        return ResponseEntity.ok(certificateService.getStatus(certificateRequest));
    }

    @PostMapping({"/program/download", "/download"})
    public ResponseEntity<AppResponse<Map<String, Object>>> download(
            @Valid @RequestBody CertificateRequest certificateRequest) {
        return ResponseEntity.ok(certificateService.download(certificateRequest));
    }

    @PostMapping("/competency/status")
    public ResponseEntity<AppResponse<List<Map<String, Object>>>> competencyStatus(
            @Valid @RequestBody CompetencyCertificateRequest request) {
        return ResponseEntity.ok(competencyCertificateQueryService.getStatus(request));
    }

    @PostMapping("/competency/download")
    public ResponseEntity<AppResponse<List<Map<String, Object>>>> competencyDownload(
            @Valid @RequestBody CompetencyCertificateRequest request) {
        return ResponseEntity.ok(competencyCertificateQueryService.download(request));
    }
}