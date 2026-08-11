package org.aastrika.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SunbirdApiBatchResp {
    private List<String> createdFor;
    private String endDate;
    private String name;
    private String batchId;
    private String enrollmentType;
    private String enrollmentEndDate;
    private String startDate;
    private int status;
}

