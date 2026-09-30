package org.aastrika.dto.response;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

/**
 * One review in a lookup page. {@code updatedOnUuid} is the raw {@code timeuuid} — pass it back as
 * the {@code updateOn} cursor to fetch the next page.
 */
@Data
@Builder
public class RatingLookupResponse {

    private String activityId;
    private String activityType;
    private String userId;
    private String firstName;
    private Float rating;
    private String review;
    private Instant updatedOn;
    private String updatedOnUuid;
}
