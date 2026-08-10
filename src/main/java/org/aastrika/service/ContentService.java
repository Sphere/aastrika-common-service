package org.aastrika.service;


import org.aastrika.dto.response.SunbirdApiResp;
import org.aastrika.dto.response.SunbirdApiUserCourseListResp;

import java.util.List;
import java.util.Map;


public interface ContentService {

     Map<String, Object> searchLiveContent(String contentId);

     Map<String, Object> getHierarchyResponseMap(String contentId);

     String getParentIdentifier(String resourceId);

     String getContentType(String resourceId);
}

