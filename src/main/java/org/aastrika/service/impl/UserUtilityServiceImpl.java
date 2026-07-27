package org.aastrika.service.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aastrika.common.Constants;
import org.aastrika.common.ServerConfig;
import org.aastrika.service.UserUtilityService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class UserUtilityServiceImpl implements UserUtilityService {

    @Autowired
    OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    @Autowired
    ServerConfig serverConfig;

    @Override
    public Map<String, Object> getUsersReadData(String userId, String authToken, String userAuthToken) {
        Map<String, String> header = new HashMap<>();
        if (StringUtils.isNotEmpty(authToken)) {
            header.put(Constants.AUTH_TOKEN, authToken);
        }
        if (StringUtils.isNotEmpty(userAuthToken)) {
            header.put(Constants.X_AUTH_TOKEN, userAuthToken);
        }
        Map<String, Object> readData = (Map<String, Object>) outboundRequestHandlerService
                .fetchUsingGetWithHeadersProfile(serverConfig.getSbUrl() + serverConfig.getLmsUserReadPath() + userId,
                        header);
        Map<String, Object> result = (Map<String, Object>) readData.get(Constants.RESULT);
        Map<String, Object> responseMap = (Map<String, Object>) result.get(Constants.RESPONSE);
        return responseMap;
    }



}

