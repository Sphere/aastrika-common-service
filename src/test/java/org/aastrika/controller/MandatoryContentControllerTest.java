package org.aastrika.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.aastrika.dto.response.AppResponse;
import org.aastrika.exception.ApiException;
import org.aastrika.service.MandatoryContentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP-layer tests for the mandatory-content status check. All four headers are required, and this
 * endpoint uses {@code xAuthUser} / {@code wid} rather than the passbook identity header.
 */
@WebMvcTest(MandatoryContentController.class)
class MandatoryContentControllerTest {

    private static final String TOKEN = "abc.def.ghi";
    private static final String ROOT_ORG = "aastrika";
    private static final String ORG = "org-1";
    private static final String WID = "user-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MandatoryContentService mandatoryContentService;

    private static AppResponse<Map<String, Object>> ok(boolean completed) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Successful");
        result.put("response", Map.of("allMandatoryContentCompleted", completed));
        return AppResponse.success("api.mandatory.content.status", result, HttpStatus.OK);
    }

    @Test
    void status_returns200AndPassesAllFourHeaders() throws Exception {
        when(mandatoryContentService.getMandatoryContentStatusForUser(any(), any(), any(), any()))
                .thenReturn(ok(true));

        mockMvc.perform(get("/v1/check/mandatoryContentStatus")
                        .header("xAuthUser", TOKEN)
                        .header("rootOrg", ROOT_ORG)
                        .header("org", ORG)
                        .header("wid", WID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("OK"))
                .andExpect(jsonPath("$.result.response.allMandatoryContentCompleted").value(true));

        verify(mandatoryContentService).getMandatoryContentStatusForUser(TOKEN, ROOT_ORG, ORG, WID);
    }

    @Test
    void status_incompleteUser_returns200WithFalse() throws Exception {
        when(mandatoryContentService.getMandatoryContentStatusForUser(any(), any(), any(), any()))
                .thenReturn(ok(false));

        mockMvc.perform(get("/v1/check/mandatoryContentStatus")
                        .header("xAuthUser", TOKEN)
                        .header("rootOrg", ROOT_ORG)
                        .header("org", ORG)
                        .header("wid", WID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.response.allMandatoryContentCompleted").value(false));
    }

    @Test
    void status_missingXAuthUserHeader_returns400() throws Exception {
        mockMvc.perform(get("/v1/check/mandatoryContentStatus")
                        .header("rootOrg", ROOT_ORG)
                        .header("org", ORG)
                        .header("wid", WID))
                .andExpect(status().isBadRequest());

        verify(mandatoryContentService, never())
                .getMandatoryContentStatusForUser(any(), any(), any(), any());
    }

    @Test
    void status_missingWidHeader_returns400() throws Exception {
        mockMvc.perform(get("/v1/check/mandatoryContentStatus")
                        .header("xAuthUser", TOKEN)
                        .header("rootOrg", ROOT_ORG)
                        .header("org", ORG))
                .andExpect(status().isBadRequest());

        verify(mandatoryContentService, never())
                .getMandatoryContentStatusForUser(any(), any(), any(), any());
    }

    @Test
    void status_missingOrgHeader_returns400() throws Exception {
        mockMvc.perform(get("/v1/check/mandatoryContentStatus")
                        .header("xAuthUser", TOKEN)
                        .header("rootOrg", ROOT_ORG)
                        .header("wid", WID))
                .andExpect(status().isBadRequest());

        verify(mandatoryContentService, never())
                .getMandatoryContentStatusForUser(any(), any(), any(), any());
    }

    @Test
    void status_upstreamFailure_surfacesErrorEnvelope() throws Exception {
        when(mandatoryContentService.getMandatoryContentStatusForUser(any(), any(), any(), any()))
                .thenThrow(new ApiException("api.mandatory.content.status", HttpStatus.BAD_GATEWAY,
                        "Failed to read course progress"));

        mockMvc.perform(get("/v1/check/mandatoryContentStatus")
                        .header("xAuthUser", TOKEN)
                        .header("rootOrg", ROOT_ORG)
                        .header("org", ORG)
                        .header("wid", WID))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.params.errmsg").value("Failed to read course progress"));
    }
}
