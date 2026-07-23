package org.aastrika.dto.response;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * One user's competency passbook, keyed by {@code competencyId}. This is the per-user element of
 * the {@code content} list returned by the passbook read APIs.
 */
@Getter
@Setter
public class CompetencyPassbookInfo {

    private final String userId;
    private String typeName = "competency";
    private Map<String, CompetencyInfo> competencies = new LinkedHashMap<>();

    public CompetencyPassbookInfo(String userId) {
        this.userId = userId;
    }
}
