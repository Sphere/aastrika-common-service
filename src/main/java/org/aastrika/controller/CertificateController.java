package org.aastrika.controller;

import jakarta.validation.Valid;
import org.aastrika.service.CertificateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/certificate")
public class CertificateController {

    private static final String SQL = """
        SELECT is_completed, is_certificate_generated, certificate_url
        FROM user_program_completion
        WHERE root_org_id = ? AND program_id = ? AND user_id = ?""";

    private JdbcTemplate jdbc;

    @PostMapping("/status")
    public ResponseEntity<?> status(@Valid @RequestBody CertificateRequest req) {

        var rows = jdbc.queryForList(
                SQL,
                req.rootOrgId(),
                req.programId(),
                req.userId()
        );

        if (rows.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "RESOURCE_NOT_FOUND"));
        }

        var r = rows.get(0);

        return ResponseEntity.ok(Map.of(
                "rootOrgId", req.rootOrgId(),
                "userId", req.userId(),
                "programId", req.programId(),
                "isCompleted", r.get("is_completed"),
                "isCertificateGenerated", r.get("is_certificate_generated")
        ));
    }

    @PostMapping("/download")
    public ResponseEntity<?> download(@Valid @RequestBody CertificateRequest req) {

        var rows = jdbc.queryForList(
                SQL,
                req.rootOrgId(),
                req.programId(),
                req.userId()
        );

        if (rows.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "RESOURCE_NOT_FOUND"));
        }

        var r = rows.get(0);

        if (!Boolean.TRUE.equals(r.get("is_completed"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "PROGRAM_NOT_COMPLETED"));
        }

        String url = (String) r.get("certificate_url");

        if (url == null || url.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "CERTIFICATE_NOT_GENERATED"));
        }

        return ResponseEntity.ok(Map.of(
                "rootOrgId", req.rootOrgId(),
                "userId", req.userId(),
                "programId", req.programId(),
                "certificateUrl", url
        ));    }
    // ok()/error() build the envelope: id="api.program.certification", ver="v1",
    // ts=String.valueOf(System.currentTimeMillis()), params=null, responseCode, result
}
