package org.aastrika.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

/**
 * v3 of {@link RatingInfo} — byte-compatible with the source's raw Cassandra row passthrough:
 * lowercase column-name keys, and the {@code *on} timeuuid columns returned as their raw UUID
 * string (not resolved to an {@code Instant}), matching the existing sunbird-cb-ext read response.
 */
@Data
@Builder
public class RatingInfoV3 {

    @JsonProperty("activityid")
    private String activityId;

    @JsonProperty("activitytype")
    private String activityType;

    private String userId;
    private Float rating;
    private String review;
    private String comment;

    @JsonProperty("commentby")
    private String commentBy;

    private String recommended;

    @JsonProperty("createdon")
    private String createdOn;

    @JsonProperty("updatedon")
    private String updatedOn;

    @JsonProperty("commentupdatedon")
    private String commentUpdatedOn;
}