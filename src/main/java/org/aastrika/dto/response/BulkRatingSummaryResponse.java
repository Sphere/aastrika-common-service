package org.aastrika.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Per-activity rating summary, migrated from the recommendation service's {@code RatingSummary}
 * model. Field types (plain {@code int}/{@code double}, not {@code Float}) match the source
 * exactly so the JSON shape stays byte-compatible for the existing external caller.
 */
@Data
@Builder
public class BulkRatingSummaryResponse {

    private String activityId;
    private String activityType;
    private int sumOfTotalRatings;
    private int totalNumberOfRatings;
    private double averageRating;
}