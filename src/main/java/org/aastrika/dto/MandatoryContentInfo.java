package org.aastrika.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MandatoryContentInfo {
    private String rootOrg;
    private String org;
    private String contentType;
    private String batchId;
    private Float minProgressForCompletion = 0.0f;

    private Float userProgress = 0.0f;
}

