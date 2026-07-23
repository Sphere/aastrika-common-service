package org.aastrika.service;

import java.util.Map;

import org.aastrika.dto.request.RatingsLookupRequest;
import org.aastrika.dto.request.RatingsReadRequest;
import org.aastrika.dto.request.RequestRating;
import org.aastrika.dto.response.AppResponse;

public interface RatingService {

    /**
     * A single user's rating for an activity. Returns a 200 with a {@code null} result when no
     * rating exists (matching the source's read-is-not-an-error behaviour).
     */
    AppResponse<Map<String, Object>> getRating(String activityId, String activityType, String userId);

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

    /**
     * Ops job: pushes each activity's aggregate rating stats (avg + star counts) from
     * {@code ratings_summary} into content metadata via the content system-update API.
     */
    AppResponse<Map<String, Object>> updateRatingsMetaData();

    /**
     * Ops job: adds/removes an additional tag ({@code mostEnrolled} / {@code mostTrending}) on content,
     * driven by the course-id list held in Redis.
     *
     * @throws org.aastrika.exception.ApiException 400 if the tag is not supported
     */
    AppResponse<Map<String, Object>> updateAdditionalTag(String tag);
}
