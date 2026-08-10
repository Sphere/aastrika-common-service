package org.aastrika.dto.response;


import lombok.Getter;
import lombok.Setter;
import org.aastrika.dto.SunbirdApiHierarchyResultBatch;
import org.aastrika.dto.SunbirdApiHierarchyResultContent;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SunbirdApiRespResult {
    private SunbirdApiResultResponse response;
    private SunbirdApiHierarchyResultContent content;
    private SunbirdApiHierarchyResultBatch batch;
    private Map<String, Object> questionSet;
    private List<Map<String, Object>> questions;
}


