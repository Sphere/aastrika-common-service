package org.aastrika.service;


import io.swagger.v3.oas.annotations.servers.Server;
import org.aastrika.dto.LastLoginInfo;
import org.aastrika.dto.UserRegistration;

import java.util.List;
import java.util.Map;

public interface UserUtilityService {

    public boolean assignRole(String sbOrgId, String userId, String objectDetails);

    Map<String, Object> getUsersDataFromUserIds(List<String> userIds, List<String> fields, String authToken);

}