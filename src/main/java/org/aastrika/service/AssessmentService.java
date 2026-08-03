package org.aastrika.service;

import java.util.Map;

import org.aastrika.dto.request.AssessmentSubmitRequest;

public interface AssessmentService {

    /**
     * Validates the user (external user-search), scores the submitted questions in-memory, resolves
     * the parent content, and persists the attempt (assessment or quiz path).
     *
     * <p>Returns the raw score summary ({@code result, correct, inCorrect, blank, total, passPercent})
     * exactly as the source does — this is a live client contract, so it is <b>not</b> wrapped in the
     * AppResponse envelope.
     *
     * @throws org.aastrika.exception.ApiException 400 if the user is invalid
     */
    Map<String, Object> submitAssessment(String rootOrg, String userId, AssessmentSubmitRequest request);
}
