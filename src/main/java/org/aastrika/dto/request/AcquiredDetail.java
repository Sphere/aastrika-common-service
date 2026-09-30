package org.aastrika.dto.request;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * How/when a competency was acquired. {@code acquiredChannel} and {@code competencyLevelId} map to
 * clustering-key columns, so they are mandatory. {@code effectiveDate} is optional (defaults to
 * "now"); accepts ISO-8601 (e.g. {@code 2024-01-01T12:00:00Z}) or {@code yyyy-MM-dd HH:mm:ss}.
 * Entries in {@code additionalParams} are persisted into the {@code acquireddetails} column
 * (non-string values are JSON-serialized), alongside a server-set {@code createdBy}/{@code createdDate}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcquiredDetail {

    @JsonProperty("acquiredChannel")
    @NotBlank
    private String acquiredChannel;

    @JsonProperty("competencyLevelId")
    @NotBlank
    private String competencyLevelId;

    @JsonProperty("effectiveDate")
    private String effectiveDate;

    @JsonProperty("additionalParams")
    private Map<String, Object> additionalParams;
}
