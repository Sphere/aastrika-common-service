package org.aastrika.service;


import io.swagger.v3.oas.annotations.servers.Server;
import org.aastrika.dto.LastLoginInfo;
import org.aastrika.dto.UserRegistration;

import java.util.List;
import java.util.Map;

public interface UserUtilityService {

    Map<String, Object> getUsersReadData(String userId, String authToken, String X_authToken);
}