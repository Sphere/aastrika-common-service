package org.aastrika.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BatchEnrolment {
    private String batchId;
    private List<String> userList;
    }


