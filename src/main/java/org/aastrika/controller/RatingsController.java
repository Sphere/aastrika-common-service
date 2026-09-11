package org.aastrika.controller;

import java.util.Map;

import jakarta.validation.Valid;
import org.aastrika.dto.request.RatingsLookupRequest;
import org.aastrika.dto.request.RatingsReadRequest;
import org.aastrika.dto.request.RequestRating;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.service.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RatingsController {

    private final RatingService ratingService;

    public RatingsController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    /** Create or update a user's rating for an activity. */
    @PostMapping("/ratings/v1/upsert")
    public ResponseEntity<AppResponse<Map<String, Object>>> upsertRating(
            @Valid @RequestBody RequestRating requestRating) {
        return ResponseEntity.ok(ratingService.upsertRating(requestRating));
    }

    /** Batch read: ratings for one activity across a list of users. */
    @PostMapping("/ratings/v3/read")
    public ResponseEntity<AppResponse<Map<String, Object>>> readRatings(
            @Valid @RequestBody RatingsReadRequest request) {
        return ResponseEntity.ok(ratingService.readRatings(request));
    }

    /**
     * v3: same computation as v2, but the response matches the source's raw Cassandra row shape
     * exactly (lowercase column-name keys, {@code createdon}/{@code updatedon}/
     * {@code commentupdatedon} as raw timeuuid strings) to match sunbird-cb-ext's response.
     */
    @PostMapping("/ratings/v2/read")
    public ResponseEntity<AppResponse<Map<String, Object>>> readRatingsV3(
            @Valid @RequestBody RatingsReadRequest request) {
        return ResponseEntity.ok(ratingService.readRatingsV3(request));
    }

    /** Aggregate rating summary (star counts + latest reviews) for an activity. */
    @GetMapping("/ratings/v2/summary/{activityId}/{activityType}")
    public ResponseEntity<AppResponse<Map<String, Object>>> getRatingSummary(
            @PathVariable("activityId") String activityId,
            @PathVariable("activityType") String activityType) {
        return ResponseEntity.ok(ratingService.getRatingSummary(activityId, activityType));
    }

    /**
     * v2: same computation as v1, but the response fields match the source {@code SummaryModel}
     * exactly (lowercase/snake_case star-count keys, {@code latest50Reviews} as a double-encoded
     * JSON string) — for an existing external caller that parses this exact shape.
     */
    @GetMapping("/ratings/v1/summary/{activityId}/{activityType}")
    public ResponseEntity<AppResponse<Map<String, Object>>> getRatingSummaryV2(
            @PathVariable("activityId") String activityId,
            @PathVariable("activityType") String activityType) {
        return ResponseEntity.ok(ratingService.getRatingSummaryV2(activityId, activityType));
    }

    /** Paginated list of reviews for an activity (optionally filtered by star). */
    @PostMapping("/ratings/v2/ratingLookUp")
    public ResponseEntity<AppResponse<Map<String, Object>>> ratingLookUp(
            @Valid @RequestBody RatingsLookupRequest request) {
        return ResponseEntity.ok(ratingService.ratingLookUp(request));
    }

    /**
     * v2: same computation as v1, response keys renamed ({@code updatedOn} → {@code updatedon},
     * {@code updatedOnUuid} → {@code updatedOnUUID}) to match the existing UI integration.
     */
    @PostMapping("/ratings/v1/ratingLookUp")
    public ResponseEntity<AppResponse<Map<String, Object>>> ratingLookUpV2(
            @Valid @RequestBody RatingsLookupRequest request) {
        return ResponseEntity.ok(ratingService.ratingLookUpV2(request));
    }

}
