package org.aastrika.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Batch read of ratings for one activity across several users. Body shape matches the source repo
 * verbatim: a {@code request} envelope whose {@code userId} key holds a list. The Java field stays
 * {@code userIds} for readability; the wire name is pinned by {@code @JsonProperty} because the
 * app integration depends on it (consistent with the passbook DTOs).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingsReadRequest {

    @JsonProperty("request")
    @NotNull
    @Valid
    private Payload request;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        @JsonProperty("activityId")
        @NotBlank
        private String activityId;

        @JsonProperty("activityType")
        @NotBlank
        private String activityType;

        @JsonProperty("userId")
        @NotEmpty
        private List<String> userIds;
    }
}
