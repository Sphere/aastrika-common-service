package org.aastrika.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aastrika.common.Constants;
import org.aastrika.dto.request.PassbookReadRequest;
import org.aastrika.dto.request.PassbookUpdateRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.service.PassbookService;
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
 * HTTP-layer tests for the three passbook endpoints. Note the identity header is
 * {@code Constants.X_AUTH_USER_ID} — referenced via the constant rather than a literal so these
 * tests stay correct if the header name changes.
 */
@WebMvcTest(PassbookController.class)
class PassbookControllerTest {

    private static final String ACTING_USER = "admin-hdr-user";
    private static final String TARGET_USER = "demo-user-01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PassbookService passbookService;

    private static AppResponse<Map<String, Object>> ok() {
        return AppResponse.success("api.passbook", Map.of("message", "Successful"), HttpStatus.OK);
    }

    /** Every passbook body sits inside the source repo's {@code request} envelope. */
    private static Map<String, Object> wrap(Object payload) {
        return Map.of("request", payload);
    }

    /** The inner payload only — call sites pass it through {@link #wrap(Object)}. */
    private static Map<String, Object> validUpdateBody() {
        Map<String, Object> acquired = new LinkedHashMap<>();
        acquired.put("acquiredChannel", "self");
        acquired.put("competencyLevelId", "level-1");

        Map<String, Object> competency = new LinkedHashMap<>();
        competency.put("competencyId", "comp-1");
        competency.put("acquiredDetails", acquired);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", TARGET_USER);
        body.put("typeName", "competency");
        body.put("competencyDetails", List.of(competency));
        return body;
    }

    // ------------------------------------------------- POST /user/v1/passbook

    @Test
    void readOwnPassbook_returns200AndPassesHeaderUserId() throws Exception {
        when(passbookService.getPassbook(any(), any())).thenReturn(ok());

        mockMvc.perform(post("/user/v1/passbook")
                        .header(Constants.X_AUTH_USER_ID, ACTING_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrap(Map.of("typeName", "competency")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("OK"));

        ArgumentCaptor<PassbookReadRequest> captor = ArgumentCaptor.forClass(PassbookReadRequest.class);
        verify(passbookService).getPassbook(eq(ACTING_USER), captor.capture());
        assertThat(captor.getValue().getRequest().getTypeName()).isEqualTo("competency");
    }

    /** Guards the source contract: the payload must sit under a {@code request} key. */
    @Test
    void readOwnPassbook_unwrappedBody_returns400() throws Exception {
        mockMvc.perform(post("/user/v1/passbook")
                        .header(Constants.X_AUTH_USER_ID, ACTING_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("typeName", "competency"))))
                .andExpect(status().isBadRequest());

        verify(passbookService, never()).getPassbook(any(), any());
    }

    @Test
    void readOwnPassbook_missingIdentityHeader_returns400() throws Exception {
        mockMvc.perform(post("/user/v1/passbook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrap(Map.of("typeName", "competency")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("Bad Request"));

        verify(passbookService, never()).getPassbook(any(), any());
    }

    @Test
    void readOwnPassbook_blankTypeName_returns400() throws Exception {
        mockMvc.perform(post("/user/v1/passbook")
                        .header(Constants.X_AUTH_USER_ID, ACTING_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrap(Map.of("typeName", " ")))))
                .andExpect(status().isBadRequest());

        verify(passbookService, never()).getPassbook(any(), any());
    }

    // ------------------------------------------------ PATCH /user/v1/passbook

    @Test
    void updatePassbook_returns200AndRecordsActingUserSeparately() throws Exception {
        when(passbookService.updatePassbook(any(), any())).thenReturn(ok());

        mockMvc.perform(patch("/user/v1/passbook")
                        .header(Constants.X_AUTH_USER_ID, ACTING_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrap(validUpdateBody()))))
                .andExpect(status().isOk());

        ArgumentCaptor<PassbookUpdateRequest> captor = ArgumentCaptor.forClass(PassbookUpdateRequest.class);
        verify(passbookService).updatePassbook(eq(ACTING_USER), captor.capture());
        // The header user is the actor; the body user is the subject. They are distinct on purpose.
        assertThat(captor.getValue().getRequest().getUserId()).isEqualTo(TARGET_USER);
        assertThat(captor.getValue().getRequest().getCompetencyDetails()).hasSize(1);
    }

    /** Guards the source contract: the payload must sit under a {@code request} key. */
    @Test
    void updatePassbook_unwrappedBody_returns400() throws Exception {
        mockMvc.perform(patch("/user/v1/passbook")
                        .header(Constants.X_AUTH_USER_ID, ACTING_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateBody())))
                .andExpect(status().isBadRequest());

        verify(passbookService, never()).updatePassbook(any(), any());
    }

    @Test
    void updatePassbook_emptyCompetencyDetails_returns400() throws Exception {
        Map<String, Object> body = validUpdateBody();
        body.put("competencyDetails", List.of());

        mockMvc.perform(patch("/user/v1/passbook")
                        .header(Constants.X_AUTH_USER_ID, ACTING_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrap(body))))
                .andExpect(status().isBadRequest());

        verify(passbookService, never()).updatePassbook(any(), any());
    }

    /** @Valid must cascade into the nested competency/acquired objects. */
    @Test
    void updatePassbook_nestedAcquiredDetailsMissing_returns400() throws Exception {
        Map<String, Object> competency = new LinkedHashMap<>();
        competency.put("competencyId", "comp-1");
        // acquiredDetails omitted — @NotNull @Valid on the nested field

        Map<String, Object> body = validUpdateBody();
        body.put("competencyDetails", List.of(competency));

        mockMvc.perform(patch("/user/v1/passbook")
                        .header(Constants.X_AUTH_USER_ID, ACTING_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrap(body))))
                .andExpect(status().isBadRequest());

        verify(passbookService, never()).updatePassbook(any(), any());
    }

    @Test
    void updatePassbook_nestedBlankCompetencyLevelId_returns400() throws Exception {
        Map<String, Object> acquired = new LinkedHashMap<>();
        acquired.put("acquiredChannel", "self");
        acquired.put("competencyLevelId", "");

        Map<String, Object> competency = new LinkedHashMap<>();
        competency.put("competencyId", "comp-1");
        competency.put("acquiredDetails", acquired);

        Map<String, Object> body = validUpdateBody();
        body.put("competencyDetails", List.of(competency));

        mockMvc.perform(patch("/user/v1/passbook")
                        .header(Constants.X_AUTH_USER_ID, ACTING_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrap(body))))
                .andExpect(status().isBadRequest());

        verify(passbookService, never()).updatePassbook(any(), any());
    }

    // ------------------------------------------- POST /admin/user/v1/passbook

    @Test
    void adminRead_returns200AndNeedsNoIdentityHeader() throws Exception {
        when(passbookService.getPassbookByAdmin(any())).thenReturn(ok());

        // Source contract: the list of users travels under the singular key "userId".
        String body = objectMapper.writeValueAsString(wrap(Map.of(
                "typeName", "competency",
                "userId", List.of(TARGET_USER, "demo-user-02"))));

        mockMvc.perform(post("/admin/user/v1/passbook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(passbookService).getPassbookByAdmin(any());
    }

    /** Guards the source contract: the wire key is userId (a list), not userIds. */
    @Test
    void adminRead_pluralUserIdsKey_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(wrap(Map.of(
                "typeName", "competency",
                "userIds", List.of(TARGET_USER))));

        mockMvc.perform(post("/admin/user/v1/passbook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(passbookService, never()).getPassbookByAdmin(any());
    }

    @Test
    void adminRead_emptyUserIds_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(wrap(Map.of(
                "typeName", "competency",
                "userId", List.of())));

        mockMvc.perform(post("/admin/user/v1/passbook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(passbookService, never()).getPassbookByAdmin(any());
    }
}
