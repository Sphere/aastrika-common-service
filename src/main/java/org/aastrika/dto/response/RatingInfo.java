package org.aastrika.dto.response;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

/**
 * A single user's rating/review for an activity, with the {@code timeuuid} columns resolved to
 * {@link Instant} timestamps (the source repo returned raw epoch millis).
 */
@Data
@Builder
public class RatingInfo {

    private String activityId;
    private String activityType;
    private String userId;
    private Float rating;
    private String review;
    private String comment;
    private String commentBy;
    private String recommended;
    private Instant createdOn;
    private Instant updatedOn;
    private Instant commentUpdatedOn;
}
