package org.aastrika.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Competency certificate status/download:
 * {@code {"request": {"filters": {"rootOrgId", "userId", "competencyId"}}}}.
 *
 * <p>Every filter is optional; the given ones are combined (AND). At least one is required —
 * enforced in the service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyCertificateRequest {

    @JsonProperty("request")
    @NotNull
    @Valid
    private Payload request;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        @JsonProperty("filters")
        private Filters filters;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Filters {

        @JsonProperty("rootOrgId")
        private String rootOrgId;

        @JsonProperty("userId")
        private String userId;

        @JsonProperty("competencyId")
        private String competencyId;
    }
}
