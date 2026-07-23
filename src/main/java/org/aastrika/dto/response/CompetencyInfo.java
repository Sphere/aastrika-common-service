package org.aastrika.dto.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * A single competency within a {@link CompetencyPassbookInfo}, with the list of times it was
 * acquired (newest first). Each acquired entry is a flat map so the shape stays open-ended per
 * competency type.
 */
@Getter
@Setter
public class CompetencyInfo {

    private final String competencyId;
    private Map<String, String> additionalParams;
    private List<Map<String, Object>> acquiredDetails = new ArrayList<>();

    public CompetencyInfo(String competencyId) {
        this.competencyId = competencyId;
    }
}
