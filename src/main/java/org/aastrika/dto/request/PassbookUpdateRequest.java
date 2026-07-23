package org.aastrika.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Add passbook entries for {@code userId}. The acting user (from the {@code x-authenticated-userid}
 * header) is recorded as {@code createdBy} on each entry, not as the owner.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassbookUpdateRequest {

    @JsonProperty("userId")
    @NotBlank
    private String userId;

    @JsonProperty("typeName")
    @NotBlank
    private String typeName;

    @JsonProperty("competencyDetails")
    @NotEmpty
    @Valid
    private List<CompetencyDetail> competencyDetails;
}
