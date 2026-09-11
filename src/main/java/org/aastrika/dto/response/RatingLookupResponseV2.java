package org.aastrika.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

/**
 * v2 of {@link RatingLookupResponse} — same fields, but {@code updatedOn}/{@code updatedOnUuid} are
 * serialized as {@code updatedon}/{@code updatedOnUUID} to match the existing UI integration's
 * expected response keys.
 */
@Data
@Builder
public class RatingLookupResponseV2 {

    private String activityId;
    private String activityType;
    private String userId;
    private String firstName;
    private Float rating;
    private String review;

    @JsonProperty("updatedon")
    private Instant updatedOn;

    @JsonProperty("updatedOnUUID")
    private String updatedOnUuid;
}