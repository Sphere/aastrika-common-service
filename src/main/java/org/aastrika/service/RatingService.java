package org.aastrika.service;

import java.util.Map;

import org.aastrika.dto.request.RatingsLookupRequest;
import org.aastrika.dto.request.RatingsReadRequest;
import org.aastrika.dto.request.RequestRating;
import org.aastrika.dto.response.AppResponse;

public interface RatingService {

    /**
     * Create or update a user's rating for an activity. A rating add/update also emits a Kafka event
     * for the aggregation pipeline; a comment-only update does not.
     */
    AppResponse<Map<String, Object>> upsertRating(RequestRating requestRating);

    /** Ratings for one activity across a list of users ({@code count} + {@code content}). */
    AppResponse<Map<String, Object>> readRatings(RatingsReadRequest request);

    /**
     * Aggregate rating summary (star counts + latest reviews, enriched with reviewer names) for an
     * activity. Returns 200 with a {@code null} response when no summary row exists.
     */
    AppResponse<Map<String, Object>> getRatingSummary(String activityId, String activityType);

    /** A paginated page of reviews for an activity (newest first), enriched with reviewer names. */
    AppResponse<Map<String, Object>> ratingLookUp(RatingsLookupRequest request);
}
