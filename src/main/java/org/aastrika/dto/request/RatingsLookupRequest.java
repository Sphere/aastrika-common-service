package org.aastrika.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Paginated lookup of individual reviews for an activity. {@code rating} is an optional star filter
 * (it's part of the {@code ratings_lookup} partition key, so supplying it makes the query
 * single-partition). {@code updateOn} is the pagination cursor: the {@code updatedon} UUID of the
 * last row from the previous page; rows strictly older than it are returned next.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingsLookupRequest {

    @JsonProperty("activityId")
    @NotBlank
    private String activityId;

    @JsonProperty("activityType")
    @NotBlank
    private String activityType;

    @JsonProperty("rating")
    @DecimalMin(value = "1.0", message = "Rating must be between 1 and 5.")
    @DecimalMax(value = "5.0", message = "Rating must be between 1 and 5.")
    private Float rating;

    @JsonProperty("limit")
    @NotNull
    @Min(value = 1, message = "Limit must be greater than 1")
    private Integer limit;

    @JsonProperty("updateOn")
    private String updateOn;
}
