package org.aastrika.controller;

import jakarta.validation.Valid;
import java.util.Map;
import org.aastrika.dto.request.RegisterTokenRequest;
import org.aastrika.dto.request.RemoveTokenRequest;
import org.aastrika.dto.request.SendByTokenRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.service.PushNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/push-notification")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;

    public PushNotificationController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @PostMapping("/register-fcm-token")
    public ResponseEntity<AppResponse<Map<String, Object>>> registerToken(
            @Valid @RequestBody RegisterTokenRequest registerTokenRequest) {
        return ResponseEntity.ok(pushNotificationService.registerToken(registerTokenRequest));
    }

    @PostMapping("/send-by-token")
    public ResponseEntity<AppResponse<Map<String, Object>>> sendByToken(
            @Valid @RequestBody SendByTokenRequest sendByTokenRequest) {
        return ResponseEntity.ok(pushNotificationService.sendByToken(sendByTokenRequest));
    }

    @PostMapping("/remove-fcm-token")
    public ResponseEntity<AppResponse<Map<String, Object>>> removeToken(
            @Valid @RequestBody RemoveTokenRequest removeTokenRequest) {
        return ResponseEntity.ok(pushNotificationService.removeToken(removeTokenRequest));
    }
}