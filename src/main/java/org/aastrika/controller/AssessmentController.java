package org.aastrika.controller;

import java.util.Map;

import jakarta.validation.Valid;
import org.aastrika.dto.request.AssessmentSubmitRequest;
import org.aastrika.service.AssessmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    /**
     * Validate, score and persist an assessment/quiz submission. userId + rootOrg come from gateway
     * headers (the only consumed assessment endpoint). Returns 201 with the raw score summary.
     */
    @PostMapping("/v2/user/assessment/submit")
    public ResponseEntity<Map<String, Object>> submitUserAssessment(
            @RequestHeader("userId") String userId,
            @RequestHeader("rootOrg") String rootOrg,
            @Valid @RequestBody AssessmentSubmitRequest request) {
        return new ResponseEntity<>(assessmentService.submitAssessment(rootOrg, userId, request), HttpStatus.CREATED);
    }
}
