package org.aastrika.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.aastrika.dto.MandatoryContentInfo;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
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

