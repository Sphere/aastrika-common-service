package org.aastrika.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.aastrika.common.Constants;
import org.aastrika.dto.request.UserMigrateRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.exception.ApiException;
import org.aastrika.service.ProfileService;
import org.aastrika.service.UserBulkUploadService;
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
 * HTTP-layer tests for {@code PATCH /user/v1/migrate}. The source's {@code {"request": {...}}}
 * envelope is part of the contract here, so the shape is pinned explicitly.
 */
@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    private static final String USER_ID = "a01e7a52-eb21-4d64-8938-8d084f62380e";
    private static final String CHANNEL = "Sphere test 1";
    private static final String USER_TOKEN = "user-token";
    private static final String AUTH_TOKEN = "auth-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfileService profileService;

    // ProfileController also hosts /user/v1/bulkupload; the slice needs the bean even though these
    // tests only exercise the migrate endpoint.
    @MockitoBean
    private UserBulkUploadService userBulkUploadService;

    private static Map<String, Object> body(String userId, String channel) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (userId != null) {
            payload.put("userId", userId);
        }
        if (channel != null) {
            payload.put("channel", channel);
        }
        return Map.of("request", payload);
    }

    private static AppResponse<Map<String, Object>> ok() {
        return AppResponse.success("api.user.migrate", Map.of("response", "SUCCESS"), HttpStatus.OK);
    }

    @Test
    void migrate_validRequest_returns200AndPassesEnvelopeAndHeaders() throws Exception {
        when(profileService.migrateUser(any(), any(), any())).thenReturn(ok());

        mockMvc.perform(patch("/user/v1/migrate")
                        .header(Constants.X_AUTH_TOKEN, USER_TOKEN)
                        .header(Constants.AUTH_TOKEN, AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(USER_ID, CHANNEL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("OK"))
                .andExpect(jsonPath("$.result.response").value("SUCCESS"));

        ArgumentCaptor<UserMigrateRequest> captor = ArgumentCaptor.forClass(UserMigrateRequest.class);
        verify(profileService).migrateUser(captor.capture(), eq(USER_TOKEN), eq(AUTH_TOKEN));
        assertThat(captor.getValue().getRequest().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getRequest().getChannel()).isEqualTo(CHANNEL);
    }

    /** The envelope is the contract — a flattened body must not silently pass. */
    @Test
    void migrate_flattenedBodyWithoutRequestEnvelope_returns400() throws Exception {
        String flat = objectMapper.writeValueAsString(Map.of("userId", USER_ID, "channel", CHANNEL));

        mockMvc.perform(patch("/user/v1/migrate")
                        .header(Constants.X_AUTH_TOKEN, USER_TOKEN)
                        .header(Constants.AUTH_TOKEN, AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(flat))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).migrateUser(any(), any(), any());
    }

    @Test
    void migrate_missingUserId_returns400() throws Exception {
        mockMvc.perform(patch("/user/v1/migrate")
                        .header(Constants.X_AUTH_TOKEN, USER_TOKEN)
                        .header(Constants.AUTH_TOKEN, AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(null, CHANNEL))))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).migrateUser(any(), any(), any());
    }

    @Test
    void migrate_blankChannel_returns400() throws Exception {
        mockMvc.perform(patch("/user/v1/migrate")
                        .header(Constants.X_AUTH_TOKEN, USER_TOKEN)
                        .header(Constants.AUTH_TOKEN, AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(USER_ID, "  "))))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).migrateUser(any(), any(), any());
    }

    @Test
    void migrate_missingUserTokenHeader_returns400() throws Exception {
        mockMvc.perform(patch("/user/v1/migrate")
                        .header(Constants.AUTH_TOKEN, AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(USER_ID, CHANNEL))))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).migrateUser(any(), any(), any());
    }

    @Test
    void migrate_missingAuthorizationHeader_returns400() throws Exception {
        mockMvc.perform(patch("/user/v1/migrate")
                        .header(Constants.X_AUTH_TOKEN, USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(USER_ID, CHANNEL))))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).migrateUser(any(), any(), any());
    }

    /**
     * An invalid channel is rejected by the learner service, and its message must reach the caller
     * verbatim — that is how "Invalid value X for parameter channel" surfaces.
     */
    @Test
    void migrate_invalidChannel_surfacesDownstreamMessageAs502() throws Exception {
        when(profileService.migrateUser(any(), any(), any()))
                .thenThrow(new ApiException("api.user.migrate", HttpStatus.BAD_GATEWAY,
                        "Invalid value aastrika for parameter channel. Please provide a valid value."));

        mockMvc.perform(patch("/user/v1/migrate")
                        .header(Constants.X_AUTH_TOKEN, USER_TOKEN)
                        .header(Constants.AUTH_TOKEN, AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(USER_ID, "aastrika"))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.params.errmsg").value(
                        "Invalid value aastrika for parameter channel. Please provide a valid value."));
    }

    /** Migrated but no user row: 404, not the source's blanket 500. */
    @Test
    void migrate_userRowMissing_returns404() throws Exception {
        when(profileService.migrateUser(any(), any(), any()))
                .thenThrow(new ApiException("api.user.migrate", HttpStatus.NOT_FOUND,
                        "User is migrated but no user record was found to update"));

        mockMvc.perform(patch("/user/v1/migrate")
                        .header(Constants.X_AUTH_TOKEN, USER_TOKEN)
                        .header(Constants.AUTH_TOKEN, AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(USER_ID, CHANNEL))))
                .andExpect(status().isNotFound());
    }

    /** Migrated but unusable profileDetails: 422, not 500. */
    @Test
    void migrate_profileDetailsEmpty_returns422() throws Exception {
        when(profileService.migrateUser(any(), any(), any()))
                .thenThrow(new ApiException("api.user.migrate", HttpStatus.UNPROCESSABLE_ENTITY,
                        "User is migrated but has no profileDetails to update"));

        mockMvc.perform(patch("/user/v1/migrate")
                        .header(Constants.X_AUTH_TOKEN, USER_TOKEN)
                        .header(Constants.AUTH_TOKEN, AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(USER_ID, CHANNEL))))
                .andExpect(status().isUnprocessableEntity());
    }
}
