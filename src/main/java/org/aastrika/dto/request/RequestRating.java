package org.aastrika.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Upsert a rating/review for an activity. {@code rating} is always required (1&ndash;5), matching the
 * source. {@code comment}/{@code commentBy} carry an (admin) reply; {@code recommended} is a
 * free-form flag. Validation that was manual in the source is expressed as bean-validation here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestRating {

    @JsonProperty("activityId")
    @NotBlank
    private String activityId;

    @JsonProperty("activityType")
    @NotBlank
    private String activityType;

    @JsonProperty("userId")
    @NotBlank
    private String userId;

    @JsonProperty("rating")
    @NotNull
    @DecimalMin(value = "1.0", message = "Rating must be between 1 and 5.")
    @DecimalMax(value = "5.0", message = "Rating must be between 1 and 5.")
    private Float rating;

    @JsonProperty("review")
    @Pattern(regexp = "^[-A-Za-z0-9.!;_?@&\n\"\", ]++$", message = "Review must contain only alphanumeric string.")
    private String review;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("commentBy")
    private String commentBy;

    @JsonProperty("recommended")
    private String recommended;
}
