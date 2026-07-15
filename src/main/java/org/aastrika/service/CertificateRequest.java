package org.aastrika.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;


public record CertificateRequest(
        @JsonProperty("rootOrgId") @NotBlank String rootOrgId,
        @JsonProperty("programId") @NotBlank String programId,
        @JsonProperty("userId")    @NotBlank String userId) {}
