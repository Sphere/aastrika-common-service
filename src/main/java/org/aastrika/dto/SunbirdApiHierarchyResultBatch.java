package org.aastrika.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SunbirdApiHierarchyResultBatch {

    private int count;
    private List<String> participants;
}

