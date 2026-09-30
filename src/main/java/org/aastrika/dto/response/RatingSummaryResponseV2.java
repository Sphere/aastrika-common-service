package org.aastrika.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

/**
 * v2 rating-summary response — byte-compatible with the source {@code SummaryModel}: lowercase/
 * snake_case star-count keys, and {@code latest50Reviews} as a double-encoded JSON string (the
 * source's raw column value re-serialized after enrichment), not a real array like v1's
 * {@link RatingSummaryResponse}.
 */
@Data
@Builder
public class RatingSummaryResponseV2 {

    private String activityId;
    private String activityType;

    @JsonProperty("totalcount1stars")
    private Float totalCount1Stars;

    @JsonProperty("totalcount2stars")
    private Float totalCount2Stars;

    @JsonProperty("totalcount3stars")
    private Float totalCount3Stars;

    @JsonProperty("totalcount4stars")
    private Float totalCount4Stars;

    @JsonProperty("totalcount5stars")
    private Float totalCount5Stars;

    @JsonProperty("total_number_of_ratings")
    private Float totalNumberOfRatings;

    @JsonProperty("sum_of_total_ratings")
    private Float sumOfTotalRatings;

    /** Double-encoded JSON array string; {@code null} only when the source row has no reviews column. */
    private String latest50Reviews;
}