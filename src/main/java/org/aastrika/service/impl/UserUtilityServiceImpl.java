package org.aastrika.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.aastrika.common.Constants;
import org.aastrika.config.ServerConfig;
import org.aastrika.dto.UserRegistration;
import org.aastrika.dto.request.SunbirdApiRequest;
import org.aastrika.service.UserUtilityService;
import org.aastrika.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public boolean isUserExist(String key, String value) throws Exception {
        // request body
        SunbirdApiRequest requestObj = new SunbirdApiRequest();
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(Constants.FILTERS, new HashMap<String, Object>() {
            {
                put(key, value);
            }
        });
        requestObj.setRequest(reqMap);

        HashMap<String, String> headersValue = new HashMap<>();
        headersValue.put(Constants.CONTENT_TYPE, "application/json");
        headersValue.put(Constants.AUTHORIZATION, serverConfig.getSbApiKey());

        try {
            String url = serverConfig.getSbUrl() + serverConfig.getUserSearchEndPoint();

            Map<String, Object> response = outboundRequestHandlerService.fetchResultUsingPost(url, requestObj,
                    headersValue);
            if (response != null && "OK".equalsIgnoreCase((String) response.get("responseCode"))) {
                Map<String, Object> map = (Map<String, Object>) response.get("result");
                if (map.get("response") != null) {
                    Map<String, Object> responseObj = (Map<String, Object>) map.get("response");
                    int count = (int) responseObj.get(Constants.COUNT);
                    if (count == 0)
                        return false;
                    else
                        return true;
                }
            }
        } catch (Exception e) {
            throw new Exception("Sunbird Service ERROR: ", e);
        }
        return true;
    }

    @Override
    public Boolean isDomainAccepted(String email) {
        String emailDomain = email.split("@")[1];
        return serverConfig.getUserRegistrationDomain().contains(emailDomain);
    }

    @Override
    public boolean createUser(UserRegistration userRegistration) {
        return false;
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
