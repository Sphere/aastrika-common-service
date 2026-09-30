package org.aastrika.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRequest {

    @JsonProperty("rootOrgId")
    @NotBlank
    private String rootOrgId;

    @JsonProperty("programId")
    @NotBlank
    private String programId;

    @JsonProperty("userId")
    @NotBlank
    private String userId;
}
