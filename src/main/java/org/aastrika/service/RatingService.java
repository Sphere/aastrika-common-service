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
     * v3: same computation as {@link #readRatings}, but the response matches the source's raw
     * Cassandra row shape exactly (lowercase column-name keys, {@code createdon}/{@code updatedon}/
     * {@code commentupdatedon} as raw timeuuid strings, not resolved timestamps).
     */
    AppResponse<Map<String, Object>> readRatingsV3(RatingsReadRequest request);

    /**
     * Aggregate rating summary (star counts + latest reviews, enriched with reviewer names) for an
     * activity. Returns 200 with a {@code null} response when no summary row exists.
     */
    AppResponse<Map<String, Object>> getRatingSummary(String activityId, String activityType);

    /**
     * v2: same computation as {@link #getRatingSummary}, but the response fields match the source
     * {@code SummaryModel} exactly (lowercase/snake_case star-count keys, {@code latest50Reviews} as
     * a double-encoded JSON string) for an existing external caller that parses this exact shape.
     */
    AppResponse<Map<String, Object>> getRatingSummaryV2(String activityId, String activityType);

    /** A paginated page of reviews for an activity (newest first), enriched with reviewer names. */
    AppResponse<Map<String, Object>> ratingLookUp(RatingsLookupRequest request);

    /**
     * v2: same computation as {@link #ratingLookUp}, but the response keys are renamed
     * ({@code updatedOn} → {@code updatedon}, {@code updatedOnUuid} → {@code updatedOnUUID}) to
     * match the existing UI integration's expected shape.
     */
    AppResponse<Map<String, Object>> ratingLookUpV2(RatingsLookupRequest request);
}
