package org.aastrika.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aastrika.dto.request.AssessmentSubmitRequest;
import org.aastrika.exception.ApiException;
import org.aastrika.service.AssessmentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HTTP-layer tests for assessment submit. This is the only endpoint that returns 201 and a raw map
 * rather than the {@code AppResponse} envelope, so both are pinned here.
 */
@WebMvcTest(AssessmentController.class)
class AssessmentControllerTest {

    private static final String USER_ID = "user-1";
    private static final String ROOT_ORG = "aastrika";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssessmentService assessmentService;

    private static Map<String, Object> validBody() {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("questionId", "q1");
        question.put("selectedOption", "a");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("identifier", "do_assessment_1");
        body.put("title", "Module quiz");
        body.put("timeLimit", 600L);
        body.put("isAssessment", false);
        body.put("questions", List.of(question));
        return body;
    }

    @Test
    void submit_validRequest_returns201WithRawScoreMap() throws Exception {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("result", 33.33);
        raw.put("total", 3);
        raw.put("blank", 0);
        raw.put("correct", 1);
        when(assessmentService.submitAssessment(any(), any(), any())).thenReturn(raw);

        mockMvc.perform(post("/v2/user/assessment/submit")
                        .header("userId", USER_ID)
                        .header("rootOrg", ROOT_ORG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                // Raw map, NOT the AppResponse envelope: no responseCode / params / result wrapper.
                .andExpect(jsonPath("$.result").value(33.33))
                .andExpect(jsonPath("$.correct").value(1))
                .andExpect(jsonPath("$.responseCode").doesNotExist())
                .andExpect(jsonPath("$.params").doesNotExist());

        ArgumentCaptor<AssessmentSubmitRequest> captor = ArgumentCaptor.forClass(AssessmentSubmitRequest.class);
        verify(assessmentService).submitAssessment(eq(ROOT_ORG), eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getIdentifier()).isEqualTo("do_assessment_1");
        assertThat(captor.getValue().getIsAssessment()).isFalse();
        assertThat(captor.getValue().getQuestions()).hasSize(1);
    }

    @Test
    void submit_missingUserIdHeader_returns400() throws Exception {
        mockMvc.perform(post("/v2/user/assessment/submit")
                        .header("rootOrg", ROOT_ORG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isBadRequest());

        verify(assessmentService, never()).submitAssessment(any(), any(), any());
    }

    @Test
    void submit_missingRootOrgHeader_returns400() throws Exception {
        mockMvc.perform(post("/v2/user/assessment/submit")
                        .header("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isBadRequest());

        verify(assessmentService, never()).submitAssessment(any(), any(), any());
    }

    @Test
    void submit_emptyQuestions_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("questions", List.of());

        mockMvc.perform(post("/v2/user/assessment/submit")
                        .header("userId", USER_ID)
                        .header("rootOrg", ROOT_ORG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(assessmentService, never()).submitAssessment(any(), any(), any());
    }

    @Test
    void submit_missingIdentifier_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.remove("identifier");

        mockMvc.perform(post("/v2/user/assessment/submit")
                        .header("userId", USER_ID)
                        .header("rootOrg", ROOT_ORG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(assessmentService, never()).submitAssessment(any(), any(), any());
    }

    @Test
    void submit_missingTimeLimit_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.remove("timeLimit");

        mockMvc.perform(post("/v2/user/assessment/submit")
                        .header("userId", USER_ID)
                        .header("rootOrg", ROOT_ORG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(assessmentService, never()).submitAssessment(any(), any(), any());
    }

    /** External user validation failure surfaces through the error envelope, not the raw map. */
    @Test
    void submit_invalidUser_returns400Envelope() throws Exception {
        when(assessmentService.submitAssessment(any(), any(), any()))
                .thenThrow(new ApiException("api.assessment.submit", HttpStatus.BAD_REQUEST,
                        "Invalid user"));

        mockMvc.perform(post("/v2/user/assessment/submit")
                        .header("userId", USER_ID)
                        .header("rootOrg", ROOT_ORG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.params.errmsg").value("Invalid user"));
    }
}
