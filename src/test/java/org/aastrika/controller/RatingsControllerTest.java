package org.aastrika.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aastrika.dto.request.RatingsLookupRequest;
import org.aastrika.dto.request.RatingsReadRequest;
import org.aastrika.dto.request.RequestRating;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.service.RatingService;
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
 * HTTP-layer tests for the four implemented rating endpoints: routing, bean validation, the
 * {@code AppResponse} envelope, and the request-DTO mapping the controller hands to the service.
 * The service is mocked — aggregation behaviour is not exercised here.
 */
@WebMvcTest(RatingsController.class)
class RatingsControllerTest {

    private static final String ACTIVITY_ID = "do_1134170689871134721450";
    private static final String USER_ID = "8066f977-4b61-4232-b28e-c60a304b4d8a";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RatingService ratingService;

    private static AppResponse<Map<String, Object>> ok(String apiId, Map<String, Object> result) {
        return AppResponse.success(apiId, result, HttpStatus.OK);
    }

    private static Map<String, Object> validUpsertBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("activityId", ACTIVITY_ID);
        body.put("activityType", "Course");
        body.put("userId", USER_ID);
        body.put("rating", 4);
        body.put("recommended", "yes");
        return body;
    }

    // ---------------------------------------------------------------- upsert

    @Test
    void upsert_validRequest_returns200AndPassesFieldsToService() throws Exception {
        when(ratingService.upsertRating(any()))
                .thenReturn(ok("api.ratings.update", Map.of("message", "Successful")));

        Map<String, Object> body = validUpsertBody();
        body.put("review", "Clear and well paced.");

        mockMvc.perform(post("/ratings/v1/upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("OK"))
                .andExpect(jsonPath("$.params.status").value("OK"))
                .andExpect(jsonPath("$.result.message").value("Successful"));

        ArgumentCaptor<RequestRating> captor = ArgumentCaptor.forClass(RequestRating.class);
        verify(ratingService).upsertRating(captor.capture());
        RequestRating sent = captor.getValue();
        assertThat(sent.getActivityId()).isEqualTo(ACTIVITY_ID);
        assertThat(sent.getActivityType()).isEqualTo("Course");
        assertThat(sent.getUserId()).isEqualTo(USER_ID);
        assertThat(sent.getRating()).isEqualTo(4.0f);
        assertThat(sent.getReview()).isEqualTo("Clear and well paced.");
        assertThat(sent.getRecommended()).isEqualTo("yes");
    }

    /** Omitting review is legal — @Pattern passes on null — and must not be rejected. */
    @Test
    void upsert_withoutReview_returns200AndNullReview() throws Exception {
        when(ratingService.upsertRating(any()))
                .thenReturn(ok("api.ratings.update", Map.of("message", "Successful")));

        mockMvc.perform(post("/ratings/v1/upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpsertBody())))
                .andExpect(status().isOk());

        ArgumentCaptor<RequestRating> captor = ArgumentCaptor.forClass(RequestRating.class);
        verify(ratingService).upsertRating(captor.capture());
        assertThat(captor.getValue().getReview()).isNull();
    }

    @Test
    void upsert_ratingAboveFive_returns400AndNeverCallsService() throws Exception {
        Map<String, Object> body = validUpsertBody();
        body.put("rating", 6);

        mockMvc.perform(post("/ratings/v1/upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("Bad Request"))
                .andExpect(jsonPath("$.params.errmsg").value(
                        org.hamcrest.Matchers.containsString("Rating must be between 1 and 5.")));

        verify(ratingService, never()).upsertRating(any());
    }

    @Test
    void upsert_ratingBelowOne_returns400() throws Exception {
        Map<String, Object> body = validUpsertBody();
        body.put("rating", 0);

        mockMvc.perform(post("/ratings/v1/upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(ratingService, never()).upsertRating(any());
    }

    @Test
    void upsert_missingRating_returns400() throws Exception {
        Map<String, Object> body = validUpsertBody();
        body.remove("rating");

        mockMvc.perform(post("/ratings/v1/upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(ratingService, never()).upsertRating(any());
    }

    @Test
    void upsert_blankActivityId_returns400() throws Exception {
        Map<String, Object> body = validUpsertBody();
        body.put("activityId", "  ");

        mockMvc.perform(post("/ratings/v1/upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(ratingService, never()).upsertRating(any());
    }

    /** The review @Pattern allows only alphanumerics and a small punctuation set. */
    @Test
    void upsert_reviewWithDisallowedCharacters_returns400() throws Exception {
        Map<String, Object> body = validUpsertBody();
        body.put("review", "great (really) #1");

        mockMvc.perform(post("/ratings/v1/upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.params.errmsg").value(
                        org.hamcrest.Matchers.containsString("alphanumeric")));

        verify(ratingService, never()).upsertRating(any());
    }

    // ------------------------------------------------------------ v2 batch read

    @Test
    void read_validRequest_returns200AndPassesUserIds() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", 1);
        result.put("content", List.of(Map.of("userId", USER_ID)));
        when(ratingService.readRatings(any())).thenReturn(ok("api.ratings.read", result));

        String body = objectMapper.writeValueAsString(Map.of(
                "activityId", ACTIVITY_ID,
                "activityType", "Course",
                "userIds", List.of(USER_ID, "ghost-user")));

        mockMvc.perform(post("/ratings/v2/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.count").value(1));

        ArgumentCaptor<RatingsReadRequest> captor = ArgumentCaptor.forClass(RatingsReadRequest.class);
        verify(ratingService).readRatings(captor.capture());
        assertThat(captor.getValue().getUserIds()).containsExactly(USER_ID, "ghost-user");
    }

    @Test
    void read_emptyUserIds_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "activityId", ACTIVITY_ID,
                "activityType", "Course",
                "userIds", List.of()));

        mockMvc.perform(post("/ratings/v2/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(ratingService, never()).readRatings(any());
    }

    /** Guards the migration rename: the field is userIds (a list), not userId. */
    @Test
    void read_singularUserIdField_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "activityId", ACTIVITY_ID,
                "activityType", "Course",
                "userId", USER_ID));

        mockMvc.perform(post("/ratings/v2/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(ratingService, never()).readRatings(any());
    }

    // ---------------------------------------------------------------- summary

    @Test
    void summary_returns200AndPassesPathVariables() throws Exception {
        when(ratingService.getRatingSummary(any(), any()))
                .thenReturn(ok("api.ratings.summary", Map.of(
                        "message", "Successful",
                        "response", Map.of("activityId", ACTIVITY_ID, "totalNumberOfRatings", 4.0))));

        mockMvc.perform(get("/ratings/v1/summary/{id}/{type}", ACTIVITY_ID, "Course"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.response.totalNumberOfRatings").value(4.0));

        verify(ratingService).getRatingSummary(ACTIVITY_ID, "Course");
    }

    /**
     * A missing ratings_summary row is 200 with response=null, not 404. Easy to mistake for success —
     * pinned here so the contract cannot change silently.
     */
    @Test
    void summary_withNoSummaryRow_returns200WithNullResponse() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Successful");
        result.put("response", null);
        when(ratingService.getRatingSummary(any(), any())).thenReturn(ok("api.ratings.summary", result));

        mockMvc.perform(get("/ratings/v1/summary/{id}/{type}", "no-such-activity", "Course"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("OK"))
                .andExpect(jsonPath("$.result.response").value(org.hamcrest.Matchers.nullValue()));
    }

    // ----------------------------------------------------------- ratingLookUp

    @Test
    void lookUp_validRequest_returns200AndPassesFilters() throws Exception {
        when(ratingService.ratingLookUp(any()))
                .thenReturn(ok("api.ratings.lookup", Map.of("message", "Successful", "response", List.of())));

        String body = objectMapper.writeValueAsString(Map.of(
                "activityId", ACTIVITY_ID,
                "activityType", "Course",
                "rating", 4,
                "limit", 5));

        mockMvc.perform(post("/ratings/v1/ratingLookUp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<RatingsLookupRequest> captor = ArgumentCaptor.forClass(RatingsLookupRequest.class);
        verify(ratingService).ratingLookUp(captor.capture());
        assertThat(captor.getValue().getLimit()).isEqualTo(5);
        assertThat(captor.getValue().getRating()).isEqualTo(4.0f);
    }

    @Test
    void lookUp_limitZero_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "activityId", ACTIVITY_ID,
                "activityType", "Course",
                "limit", 0));

        mockMvc.perform(post("/ratings/v1/ratingLookUp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(ratingService, never()).ratingLookUp(any());
    }

    @Test
    void lookUp_missingLimit_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "activityId", ACTIVITY_ID,
                "activityType", "Course"));

        mockMvc.perform(post("/ratings/v1/ratingLookUp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(ratingService, never()).ratingLookUp(any());
    }

    // ------------------------------------------------- endpoints removed as unused

    /**
     * Regression guards for the endpoint-usage audit: these three were removed deliberately. If one
     * reappears, that was not intended.
     */
    @Test
    void removedEndpoints_return404() throws Exception {
        mockMvc.perform(get("/ratings/v1/read/{a}/{t}/{u}", ACTIVITY_ID, "Course", USER_ID))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/ratings/meta/update"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/update/v1/content/additionaltag").param("tag", "mostEnrolled"))
                .andExpect(status().isNotFound());
    }
}
