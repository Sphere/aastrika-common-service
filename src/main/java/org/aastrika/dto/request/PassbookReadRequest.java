package org.aastrika.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Read a single user's passbook. The user is taken from the {@code x-authenticated-userid}
 * header, so the body only carries the {@code typeName} (e.g. {@code competency}).
 *
 * <p>Replaces the source repo's untyped {@code {"request": {"typeName": ...}}} envelope with a
 * flat, validated DTO matching this project's style (see {@code CertificateRequest}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassbookReadRequest {

    @JsonProperty("typeName")
    @NotBlank
    private String typeName;
}