package org.aastrika.dto.request;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Leaderboard read request. {@code filters} keys must be {@code leaderboard_table} column names
 * (validated server-side against the entity before they reach SQL). {@code userId} is the active
 * user whose rank is returned alongside the ranked page.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardRequest {

    @JsonProperty("filters")
    @NotEmpty
    private Map<String, Object> filters;

    @JsonProperty("userId")
    @NotBlank
    private String userId;

    @JsonProperty("limit")
    @NotNull
    @Min(1)
    @Max(1000)
    private Integer limit;

    @JsonProperty("offset")
    @NotNull
    @Min(0)
    private Integer offset;
}
