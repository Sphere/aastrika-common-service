package org.aastrika.service.impl;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.aastrika.common.Constants;
import org.aastrika.config.ServerConfig;
import org.aastrika.service.UserUtilityService;
import org.aastrika.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class UserUtilityServiceImpl implements UserUtilityService {

    @Autowired
    OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    @Autowired
    ServerConfig serverConfig;

    public boolean assignRole(String sbOrgId, String userId, String objectDetails) {
        System.out.println("Isnside assign role");
        boolean retValue = false;
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> requestBody = new HashMap<String, Object>();
        requestBody.put(Constants.ORGANIZATION_ID, sbOrgId);
        requestBody.put(Constants.USER_ID, userId);
        requestBody.put(Constants.ROLES, Arrays.asList(Constants.PUBLIC));
        request.put(Constants.REQUEST, requestBody);
        Map<String, Object> readData = (Map<String, Object>) outboundRequestHandlerService.fetchResultUsingPost(
                serverConfig.getSbUrl() + serverConfig.getSbAssignRolePath(), request, ProjectUtil.getDefaultHeaders());
        if (Constants.OK.equalsIgnoreCase((String) readData.get(Constants.RESPONSE_CODE))) {
            retValue = true;
        }
        printMethodExecutionResult("AssignRole", objectDetails, retValue);
        return retValue;
    }

    @Override
    public Map<String, Object> getUsersDataFromUserIds(List<String> userIds, List<String> fields, String authToken) {
        return Map.of();
    }

    private void printMethodExecutionResult(String methodAction, String objectDetails, boolean isSuccess) {
        StringBuilder strBuilder = new StringBuilder("Action : [").append(methodAction).append("] ");
        if (isSuccess) {
            strBuilder.append(" is successfully executed. ");
        } else {
            strBuilder.append(" is failed to execute. ");
        }
        if (StringUtils.isNotEmpty(objectDetails)) {
            strBuilder.append("For Object : ").append(objectDetails);
        }
        log.info(strBuilder.toString());
    }




}
