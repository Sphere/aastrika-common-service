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

import org.aastrika.dto.request.LeaderboardRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.exception.ApiException;
import org.aastrika.service.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

/** HTTP-layer tests for the leaderboard read endpoint, including its paging bounds. */
@WebMvcTest(LeaderboardController.class)
class LeaderboardControllerTest {

    private static final String USER_ID = "83d2e709-a958-4d96-a3a1-88e7eb0b8d31";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeaderboardService leaderboardService;

    private static Map<String, Object> validBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("filters", Map.of("state", "Bihar"));
        body.put("userId", USER_ID);
        body.put("limit", 10);
        body.put("offset", 0);
        return body;
    }

    @Test
    void leaderboard_validRequest_returns200AndPassesFilters() throws Exception {
        when(leaderboardService.getLeaderboard(any())).thenReturn(
                AppResponse.success("user.leaderboard.read", Map.of("count", 5), HttpStatus.OK));

        mockMvc.perform(post("/user/v1/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.count").value(5));

        ArgumentCaptor<LeaderboardRequest> captor = ArgumentCaptor.forClass(LeaderboardRequest.class);
        verify(leaderboardService).getLeaderboard(captor.capture());
        assertThat(captor.getValue().getFilters()).containsEntry("state", "Bihar");
        assertThat(captor.getValue().getLimit()).isEqualTo(10);
        assertThat(captor.getValue().getOffset()).isEqualTo(0);
    }

    @Test
    void leaderboard_emptyFilters_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("filters", Map.of());

        mockMvc.perform(post("/user/v1/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(leaderboardService, never()).getLeaderboard(any());
    }

    @Test
    void leaderboard_limitAboveMax_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("limit", 1001);

        mockMvc.perform(post("/user/v1/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(leaderboardService, never()).getLeaderboard(any());
    }

    @Test
    void leaderboard_limitAtMax_isAccepted() throws Exception {
        when(leaderboardService.getLeaderboard(any())).thenReturn(
                AppResponse.success("user.leaderboard.read", Map.of("count", 0), HttpStatus.OK));

        Map<String, Object> body = validBody();
        body.put("limit", 1000);

        mockMvc.perform(post("/user/v1/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void leaderboard_negativeOffset_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("offset", -1);

        mockMvc.perform(post("/user/v1/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(leaderboardService, never()).getLeaderboard(any());
    }

    @Test
    void leaderboard_missingUserId_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.remove("userId");

        mockMvc.perform(post("/user/v1/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(leaderboardService, never()).getLeaderboard(any());
    }

    /** An unknown filter column is rejected by the service, not bean validation — 400 either way. */
    @Test
    void leaderboard_invalidFilterField_returns400FromService() throws Exception {
        when(leaderboardService.getLeaderboard(any()))
                .thenThrow(new ApiException("user.leaderboard.read", HttpStatus.BAD_REQUEST,
                        "Invalid filter field: dropTable"));

        Map<String, Object> body = validBody();
        body.put("filters", Map.of("dropTable", "x"));

        mockMvc.perform(post("/user/v1/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.params.errmsg").value("Invalid filter field: dropTable"));
    }
}
