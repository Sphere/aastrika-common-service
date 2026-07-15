package org.aastrika.service;


import java.util.Map;

import jakarta.validation.Valid;
import org.aastrika.dto.AssessmentSubmissionDTO;

public interface AssessmentService {
    /**
     * submits an assessment
     *
     * @param data
     * @return
     * @throws Exception
     */
    public Map<String, Object> submitAssessment(String rootOrg, @Valid AssessmentSubmissionDTO data, String userEmail)
            throws Exception;


    Map<String, Object> getAssessmentByContentUser(String rootOrg, String courseId, String userId) throws Exception;

    /**
     * submits assessments coming from iframe
     *
     * @param request
     * @return
     * @throws Exception
     */
    Map<String, Object> submitAssessmentByIframe(String rootOrg, Map<String, Object> request) throws Exception;

    /**
     * Get assement question set
     *
     * @param courseId
     * @param assessmentContentId
     * @return
     */
    public Map<String, Object> getAssessmentContent(String courseId, String assessmentContentId);

}

