package org.aastrika.dto.response;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response of the mandatory-content status check (mirrors the source {@code MandatoryContentResponse}):
 * whether the user has completed every mandatory course, plus per-course detail keyed by course id.
 */
@Data
@NoArgsConstructor
public class MandatoryContentResponse {

    private boolean mandatoryCourseCompleted;
    private Map<String, MandatoryContentInfo> contentDetails;

    public void addContentInfo(String contentId, MandatoryContentInfo contentInfo) {
        if (contentDetails == null) {
            contentDetails = new HashMap<>();
        }
        contentDetails.put(contentId, contentInfo);
    }
}
