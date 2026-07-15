package org.aastrika.controller;

import java.util.Map;

import jakarta.validation.Valid;
import org.aastrika.dto.request.CertificateRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.service.CertificateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/certificate")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostMapping("/status")
    public ResponseEntity<AppResponse<Map<String, Object>>> status(
            @Valid @RequestBody CertificateRequest certificateRequest) {
        return ResponseEntity.ok(certificateService.getStatus(certificateRequest));
    }

    @PostMapping("/download")
    public ResponseEntity<AppResponse<Map<String, Object>>> download(
            @Valid @RequestBody CertificateRequest certificateRequest) {
        return ResponseEntity.ok(certificateService.download(certificateRequest));
    }
}
