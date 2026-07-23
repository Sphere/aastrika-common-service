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
import org.springframework.web.bind.annotation.RequestParam;
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
    @PostMapping("/ratings/v2/read")
    public ResponseEntity<AppResponse<Map<String, Object>>> readRatings(
            @Valid @RequestBody RatingsReadRequest request) {
        return ResponseEntity.ok(ratingService.readRatings(request));
    }

    /** Read a single user's rating for an activity. */
    @GetMapping("/ratings/v1/read/{activityId}/{activityType}/{userId}")
    public ResponseEntity<AppResponse<Map<String, Object>>> getRating(
            @PathVariable("activityId") String activityId,
            @PathVariable("activityType") String activityType,
            @PathVariable("userId") String userId) {
        return ResponseEntity.ok(ratingService.getRating(activityId, activityType, userId));
    }

    /** Aggregate rating summary (star counts + latest reviews) for an activity. */
    @GetMapping("/ratings/v1/summary/{activityId}/{activityType}")
    public ResponseEntity<AppResponse<Map<String, Object>>> getRatingSummary(
            @PathVariable("activityId") String activityId,
            @PathVariable("activityType") String activityType) {
        return ResponseEntity.ok(ratingService.getRatingSummary(activityId, activityType));
    }

    /** Paginated list of reviews for an activity (optionally filtered by star). */
    @PostMapping("/ratings/v1/ratingLookUp")
    public ResponseEntity<AppResponse<Map<String, Object>>> ratingLookUp(
            @Valid @RequestBody RatingsLookupRequest request) {
        return ResponseEntity.ok(ratingService.ratingLookUp(request));
    }

    /** Ops job: push aggregate rating stats from ratings_summary into content metadata. */
    @PostMapping("/ratings/meta/update")
    public ResponseEntity<AppResponse<Map<String, Object>>> updateRatingsMetaData() {
        return ResponseEntity.ok(ratingService.updateRatingsMetaData());
    }

    /** Ops job: add/remove an additional tag (mostEnrolled / mostTrending) on content. */
    @PostMapping("/update/v1/content/additionaltag")
    public ResponseEntity<AppResponse<Map<String, Object>>> updateAdditionalTag(
            @RequestParam("tag") String tag) {
        return ResponseEntity.ok(ratingService.updateAdditionalTag(tag));
    }
}
