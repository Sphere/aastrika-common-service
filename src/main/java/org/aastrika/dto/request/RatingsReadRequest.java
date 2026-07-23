package org.aastrika.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Batch read of ratings for one activity across several users. The source wrapped this in a
 * {@code {"request": {...}}} envelope with a {@code userId} list; flattened here and renamed to
 * {@code userIds} for clarity (consistent with the passbook migration).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingsReadRequest {

    @JsonProperty("activityId")
    @NotBlank
    private String activityId;

    @JsonProperty("activityType")
    @NotBlank
    private String activityType;

    @JsonProperty("userIds")
    @NotEmpty
    private List<String> userIds;
}
