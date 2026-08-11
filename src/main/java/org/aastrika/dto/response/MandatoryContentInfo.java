package org.aastrika.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-course mandatory-content status. Field/JSON names mirror the source {@code MandatoryContentInfo}
 * (camelCase). {@code minProgressForCompletion} is the required minimum; {@code userProgress} is the
 * user's actual progress (filled from the course-service progress read).
 */
@Data
@NoArgsConstructor
public class MandatoryContentInfo {

    private String rootOrg;
    private String org;
    private String contentType;
    private String batchId;
    private Float minProgressForCompletion = 0.0f;
    private Float userProgress = 0.0f;
}
