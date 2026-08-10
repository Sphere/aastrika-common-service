package org.aastrika.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A course batch, as it appears both inside the km content-search result ({@code content[].batches})
 * and inside the auto-enrollment response envelope. Mirrors the source {@code SunbirdApiBatchResp}
 * (camelCase JSON, matching the platform's batch shape).
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CohortBatch {

    private List<String> createdFor;
    private String endDate;
    private String name;
    private String batchId;
    private String enrollmentType;
    private String enrollmentEndDate;
    private String startDate;
    private int status;
}
