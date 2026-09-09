package org.aastrika.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.aastrika.dto.response.AppResponse;
import org.aastrika.exception.ApiException;
import org.aastrika.service.CohortsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP-layer tests for the two cohort endpoints. The controller's only real logic is stripping the
 * optional "Bearer " prefix from the Authorization header, so that is covered explicitly along with
 * the query-parameter defaults and required headers.
 */
@WebMvcTest(CohortsController.class)
class CohortsControllerTest {

    private static final String RESOURCE_ID = "course-1";
    private static final String USER_UUID = "user-1";
    private static final String ROOT_ORG = "aastrika";
    private static final String RAW_TOKEN = "abc.def.ghi";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CohortsService cohortsService;

    private static AppResponse<Map<String, Object>> ok() {
        return AppResponse.success("api.cohorts", Map.of("message", "Successful", "response", List.of()),
                HttpStatus.OK);
    }

    // ------------------------------------------------------------ activeusers

    @Test
    void activeUsers_returns200AndAppliesDefaultCountAndFilter() throws Exception {
        when(cohortsService.getActiveUsers(any(), any(), any(), any(), anyInt(), anyBoolean())).thenReturn(ok());

        mockMvc.perform(get("/v2/resources/{resourceId}/user/{userUUID}/cohorts/activeusers",
                        RESOURCE_ID, USER_UUID)
                        .header("Authorization", RAW_TOKEN)
                        .header("rootOrg", ROOT_ORG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("OK"));

        verify(cohortsService).getActiveUsers(RAW_TOKEN, ROOT_ORG, RESOURCE_ID, USER_UUID, 50, false);
    }

    @Test
    void activeUsers_stripsBearerPrefixFromAuthorizationHeader() throws Exception {
        when(cohortsService.getActiveUsers(any(), any(), any(), any(), anyInt(), anyBoolean())).thenReturn(ok());

        mockMvc.perform(get("/v2/resources/{resourceId}/user/{userUUID}/cohorts/activeusers",
                        RESOURCE_ID, USER_UUID)
                        .header("Authorization", "Bearer " + RAW_TOKEN)
                        .header("rootOrg", ROOT_ORG))
                .andExpect(status().isOk());

        // The service must receive the bare token, never the "Bearer " prefix.
        verify(cohortsService).getActiveUsers(eq(RAW_TOKEN), any(), any(), any(), anyInt(), anyBoolean());
    }

    @Test
    void activeUsers_honoursCountAndFilterQueryParams() throws Exception {
        when(cohortsService.getActiveUsers(any(), any(), any(), any(), anyInt(), anyBoolean())).thenReturn(ok());

        mockMvc.perform(get("/v2/resources/{resourceId}/user/{userUUID}/cohorts/activeusers",
                        RESOURCE_ID, USER_UUID)
                        .header("Authorization", RAW_TOKEN)
                        .header("rootOrg", ROOT_ORG)
                        .param("count", "7")
                        .param("filter", "true"))
                .andExpect(status().isOk());

        verify(cohortsService).getActiveUsers(RAW_TOKEN, ROOT_ORG, RESOURCE_ID, USER_UUID, 7, true);
    }

    @Test
    void activeUsers_missingRootOrgHeader_returns400() throws Exception {
        mockMvc.perform(get("/v2/resources/{resourceId}/user/{userUUID}/cohorts/activeusers",
                        RESOURCE_ID, USER_UUID)
                        .header("Authorization", RAW_TOKEN))
                .andExpect(status().isBadRequest());

        verify(cohortsService, never()).getActiveUsers(any(), any(), any(), any(), anyInt(), anyBoolean());
    }

    @Test
    void activeUsers_missingAuthorizationHeader_returns400() throws Exception {
        mockMvc.perform(get("/v2/resources/{resourceId}/user/{userUUID}/cohorts/activeusers",
                        RESOURCE_ID, USER_UUID)
                        .header("rootOrg", ROOT_ORG))
                .andExpect(status().isBadRequest());

        verify(cohortsService, never()).getActiveUsers(any(), any(), any(), any(), anyInt(), anyBoolean());
    }

    /** "Nothing found" is a 404 from the service, surfaced through the error envelope. */
    @Test
    void activeUsers_serviceReportsNothingFound_returns404Envelope() throws Exception {
        when(cohortsService.getActiveUsers(any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenThrow(new ApiException("api.cohorts.activeusers", HttpStatus.NOT_FOUND,
                        "No active batches found for this resource"));

        mockMvc.perform(get("/v2/resources/{resourceId}/user/{userUUID}/cohorts/activeusers",
                        RESOURCE_ID, USER_UUID)
                        .header("Authorization", RAW_TOKEN)
                        .header("rootOrg", ROOT_ORG))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseCode").value("Not Found"))
                .andExpect(jsonPath("$.params.errmsg").value("No active batches found for this resource"));
    }

    // --------------------------------------------------------- autoenrollment

    @Test
    void autoEnrollment_returns200AndPassesStrippedToken() throws Exception {
        when(cohortsService.autoEnrollmentInCourse(any(), any(), any(), any())).thenReturn(ok());

        mockMvc.perform(get("/v1/autoenrollment/{userUUID}/{courseId}", USER_UUID, RESOURCE_ID)
                        .header("Authorization", "Bearer " + RAW_TOKEN)
                        .header("rootOrg", ROOT_ORG))
                .andExpect(status().isOk());

        verify(cohortsService).autoEnrollmentInCourse(RAW_TOKEN, ROOT_ORG, RESOURCE_ID, USER_UUID);
    }

    @Test
    void autoEnrollment_missingRootOrgHeader_returns400() throws Exception {
        mockMvc.perform(get("/v1/autoenrollment/{userUUID}/{courseId}", USER_UUID, RESOURCE_ID)
                        .header("Authorization", RAW_TOKEN))
                .andExpect(status().isBadRequest());

        verify(cohortsService, never()).autoEnrollmentInCourse(any(), any(), any(), any());
    }

    /** A failed course-service write surfaces as 502. */
    @Test
    void autoEnrollment_courseServiceWriteFails_returns502() throws Exception {
        when(cohortsService.autoEnrollmentInCourse(any(), any(), any(), any()))
                .thenThrow(new ApiException("api.autoenrollment", HttpStatus.BAD_GATEWAY,
                        "Failed to create a batch for the course"));

        mockMvc.perform(get("/v1/autoenrollment/{userUUID}/{courseId}", USER_UUID, RESOURCE_ID)
                        .header("Authorization", RAW_TOKEN)
                        .header("rootOrg", ROOT_ORG))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.params.errmsg").value("Failed to create a batch for the course"));
    }

    // ------------------------------------------- endpoint removed as unused

    /** Regression guard: top-performers was removed by the endpoint-usage audit. */
    @Test
    void topPerformers_removed_returns404() throws Exception {
        mockMvc.perform(get("/v2/resources/{resourceId}/user/{userUUID}/cohorts/top-performers",
                        RESOURCE_ID, USER_UUID)
                        .header("Authorization", RAW_TOKEN)
                        .header("rootOrg", ROOT_ORG))
                .andExpect(status().isNotFound());
    }
}
