package org.aastrika.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bulk rating-summary lookup, migrated from the recommendation service's
 * {@code Controller.getRatings}. {@code activityType} filters the {@code ratings_summary}
 * clustering column; {@code activityIds} is the partition-key ({@code activityid}) IN list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkRatingLookupRequest {

    @JsonProperty("activityType")
    @NotBlank
    private String activityType;

    @JsonProperty("activityIds")
    @NotEmpty
    private List<String> activityIds;
}