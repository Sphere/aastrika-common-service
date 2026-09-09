package org.aastrika.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.aastrika.dto.request.CertificateRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.exception.ApiException;
import org.aastrika.service.CertificateService;
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
 * HTTP-layer tests for the certificate endpoints. Both live under the class-level
 * {@code @RequestMapping("/v1/certificate")}, so the full paths are asserted here to pin that prefix.
 */
@WebMvcTest(CertificateController.class)
class CertificateControllerTest {

    private static final String ROOT_ORG_ID = "org-1";
    private static final String PROGRAM_ID = "program-1";
    private static final String USER_ID = "user-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CertificateService certificateService;

    private static Map<String, Object> validBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rootOrgId", ROOT_ORG_ID);
        body.put("programId", PROGRAM_ID);
        body.put("userId", USER_ID);
        return body;
    }

    // ------------------------------------------------------------------ status

    @Test
    void status_returns200AndPassesAllThreeKeyFields() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rootOrgId", ROOT_ORG_ID);
        result.put("isCompleted", true);
        result.put("isCertificateGenerated", false);
        when(certificateService.getStatus(any()))
                .thenReturn(AppResponse.success("api.program.certification", result, HttpStatus.OK));

        mockMvc.perform(post("/v1/certificate/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isCompleted").value(true))
                .andExpect(jsonPath("$.result.isCertificateGenerated").value(false));

        ArgumentCaptor<CertificateRequest> captor = ArgumentCaptor.forClass(CertificateRequest.class);
        verify(certificateService).getStatus(captor.capture());
        assertThat(captor.getValue().getRootOrgId()).isEqualTo(ROOT_ORG_ID);
        assertThat(captor.getValue().getProgramId()).isEqualTo(PROGRAM_ID);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void status_blankRootOrgId_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("rootOrgId", " ");

        mockMvc.perform(post("/v1/certificate/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(certificateService, never()).getStatus(any());
    }

    @Test
    void status_missingUserId_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.remove("userId");

        mockMvc.perform(post("/v1/certificate/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(certificateService, never()).getStatus(any());
    }

    @Test
    void status_noCompletionRow_returns404() throws Exception {
        when(certificateService.getStatus(any()))
                .thenThrow(new ApiException("api.program.certification", HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND"));

        mockMvc.perform(post("/v1/certificate/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.params.errmsg").value("RESOURCE_NOT_FOUND"));
    }

    // ---------------------------------------------------------------- download

    @Test
    void download_returns200WithCertificateUrl() throws Exception {
        when(certificateService.download(any())).thenReturn(AppResponse.success(
                "api.program.certification", Map.of("certificateUrl", "https://example/cert.pdf"),
                HttpStatus.OK));

        mockMvc.perform(post("/v1/certificate/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.certificateUrl").value("https://example/cert.pdf"));

        verify(certificateService).download(any());
    }

    @Test
    void download_programNotCompleted_returns400() throws Exception {
        when(certificateService.download(any()))
                .thenThrow(new ApiException("api.program.certification", HttpStatus.BAD_REQUEST,
                        "PROGRAM_NOT_COMPLETED"));

        mockMvc.perform(post("/v1/certificate/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.params.errmsg").value("PROGRAM_NOT_COMPLETED"));
    }

    @Test
    void download_certificateNotGenerated_returns404() throws Exception {
        when(certificateService.download(any()))
                .thenThrow(new ApiException("api.program.certification", HttpStatus.NOT_FOUND,
                        "CERTIFICATE_NOT_GENERATED"));

        mockMvc.perform(post("/v1/certificate/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.params.errmsg").value("CERTIFICATE_NOT_GENERATED"));
    }

    /** The endpoints only exist under the /v1/certificate prefix. */
    @Test
    void unprefixedPaths_return404() throws Exception {
        mockMvc.perform(post("/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isNotFound());
    }
}
