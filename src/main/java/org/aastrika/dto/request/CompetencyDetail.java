package org.aastrika.dto.request;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One competency to record. {@code competencyId} maps to the {@code typeid} clustering column;
 * competency-level {@code additionalParams} are stored in the {@code additionalparams} column.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyDetail {

    @JsonProperty("competencyId")
    @NotBlank
    private String competencyId;

    @JsonProperty("acquiredDetails")
    @NotNull
    @Valid
    private AcquiredDetail acquiredDetails;

    @JsonProperty("additionalParams")
    private Map<String, String> additionalParams;
}
