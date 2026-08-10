package org.aastrika.service.impl;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.aastrika.common.Constants;
import org.aastrika.config.ServerConfig;
import org.aastrika.service.ContentService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class ContentServiceImpl implements ContentService {


    @Autowired
    private OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    @Autowired
    ServerConfig serverConfig;


    public Map<String, Object> searchLiveContent(String contentId) {
        Map<String, Object> response = null;
        HashMap<String, String> headerValues = new HashMap<>();
        headerValues.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        Map<String, Object> filters = new HashMap<>();
        filters.put(Constants.PRIMARY_CATEGORY, Arrays.asList(Constants.COURSE, Constants.PROGRAM));
        filters.put(Constants.STATUS, Arrays.asList(Constants.LIVE));
        filters.put(Constants.IDENTIFIER, contentId);
        Map<String, Object> contentRequestValue = new HashMap<>();
        contentRequestValue.put(Constants.FILTERS, filters);
        contentRequestValue.put(Constants.FIELDS, Arrays.asList(Constants.IDENTIFIER, Constants.NAME,
                Constants.PRIMARY_CATEGORY, Constants.BATCHES, Constants.LEAF_NODES_COUNT, Constants.CONTENT_TYPE_KEY));
        Map<String, Object> contentRequest = new HashMap<>();
        contentRequest.put(Constants.REQUEST, contentRequestValue);
        response = outboundRequestHandlerService.fetchResultUsingPost(
                serverConfig.getKmBaseHost() + serverConfig.getKmBaseContentSearch(), contentRequest, headerValues);
        if (null != response && Constants.OK.equalsIgnoreCase((String) response.get(Constants.RESPONSE_CODE))) {
            return response;
        }
        return null;
    }

    public Map<String, Object> getHierarchyResponseMap(String contentId) {
        StringBuilder url = new StringBuilder();
        url.append(serverConfig.getContentHost()).append(serverConfig.getHierarchyEndPoint()).append("/" + contentId)
                .append("?hierarchyType=detail");
        Map<String, Object> response = (Map<String, Object>) outboundRequestHandlerService.fetchResult(url.toString());
        if (ObjectUtils.isEmpty(response)) {
            return Collections.EMPTY_MAP;
        }

        return response;
    }

    public String getParentIdentifier(String resourceId) {
        String parentId = "";
        Map<String, Object> response = getHierarchyResponseMap(resourceId);
        if (Constants.OK.equalsIgnoreCase((String) response.get(Constants.RESPONSE_CODE))) {
            Map<String, Object> resultMap = (Map<String, Object>) response.get(Constants.RESULT);
            if (!ObjectUtils.isEmpty(resultMap)) {
                Map<String, Object> contentMap = (Map<String, Object>) resultMap.get(Constants.CONTENT);
                if (!ObjectUtils.isEmpty(contentMap)) {
                    parentId = (String) contentMap.get(Constants.PARENT);
                }
            }
        }
        return parentId;
    }

    public String getContentType(String resourceId) {
        String parentContentType = "";
        Map<String, Object> response = getHierarchyResponseMap(resourceId);
        if (Constants.OK.equalsIgnoreCase((String) response.get(Constants.RESPONSE_CODE))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) response.get(Constants.RESULT);
            if (!ObjectUtils.isEmpty(resultMap)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> contentMap = (Map<String, Object>) resultMap.get(Constants.CONTENT);
                if (!ObjectUtils.isEmpty(contentMap)) {
                    parentContentType = (String) contentMap.get(Constants.CONTENT_TYPE_KEY);
                }
            }
        }
        return parentContentType;
    }


}

