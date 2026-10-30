package org.aastrika.service;

import java.util.Map;
import org.aastrika.dto.request.RegisterTokenRequest;
import org.aastrika.dto.request.RemoveTokenRequest;
import org.aastrika.dto.request.SendByTokenRequest;
import org.aastrika.dto.response.AppResponse;

public interface PushNotificationService {

    String REGISTER_API_ID = "api.fcm.token.register";
    String SEND_API_ID = "api.fcm.notification.send";
    String REMOVE_API_ID = "api.fcm.token.remove";

    AppResponse<Map<String, Object>> registerToken(RegisterTokenRequest request);

    AppResponse<Map<String, Object>> sendByToken(SendByTokenRequest request);

    AppResponse<Map<String, Object>> removeToken(RemoveTokenRequest request);
}