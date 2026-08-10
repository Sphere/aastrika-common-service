package org.aastrika.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SunbirdApiBatch {
    private List<String> createdFor;
    private String identifier;
    private String endDate;
    private String name;
    private String batchId;
    private String enrollmentType;
    private String enrollmentEndDate;
    private String startDate;
    private int status;
}

