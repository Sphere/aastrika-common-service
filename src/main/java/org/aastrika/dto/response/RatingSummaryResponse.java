package org.aastrika.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Aggregate rating summary for an activity. {@code latest50Reviews} is returned as a real array
 * (the source double-encoded it as a JSON string), each review enriched with the reviewer's name.
 */
@Data
@Builder
public class RatingSummaryResponse {

    private String activityId;
    private String activityType;
    private Float totalCount1Stars;
    private Float totalCount2Stars;
    private Float totalCount3Stars;
    private Float totalCount4Stars;
    private Float totalCount5Stars;
    private Float totalNumberOfRatings;
    private Float sumOfTotalRatings;
    private List<Review> latest50Reviews;

    @Data
    @Builder
    public static class Review {
        private String objectType;
        private String userId;
        private Instant date;
        private Float rating;
        private String review;
        private String firstName;
    }
}
