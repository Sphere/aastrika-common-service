package org.aastrika.service.impl;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.aastrika.dto.request.RegisterTokenRequest;
import org.aastrika.dto.request.RemoveTokenRequest;
import org.aastrika.dto.request.SendByTokenRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.entity.UserFcmToken;
import org.aastrika.exception.ApiException;
import org.aastrika.repository.UserFcmTokenRepository;
import org.aastrika.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationServiceImpl.class);

    private static final String UNKNOWN_ERROR_MSG = "Unknown error";
    private static final int FCM_BATCH_SIZE = 500;
    private static final int QUERY_IN_CHUNK = 1000;

    private static final Set<MessagingErrorCode> DEAD_TOKEN_ERRORS = Set.of(
            MessagingErrorCode.UNREGISTERED,        // messaging/registration-token-not-registered
            MessagingErrorCode.INVALID_ARGUMENT);   // messaging/invalid-registration-token

    private final UserFcmTokenRepository tokenRepository;
    private final ObjectProvider<FirebaseMessaging> firebaseMessaging;

    public PushNotificationServiceImpl(UserFcmTokenRepository tokenRepository,
                                       ObjectProvider<FirebaseMessaging> firebaseMessaging) {
        this.tokenRepository = tokenRepository;
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    @Transactional
    public AppResponse<Map<String, Object>> registerToken(RegisterTokenRequest body) {
        RegisterTokenRequest.Payload request = body.getRequest();
        try {
            if ("update".equals(request.getStatus())) {
                // Remove the stale token previously registered for this user's device
                tokenRepository.deleteByUserIdAndDeviceId(request.getUserId(), request.getDeviceId());
            }
            // One row per device: a new token (or user) for the same device replaces the old one.
            OffsetDateTime now = OffsetDateTime.now();
            UserFcmToken row = tokenRepository.findById(request.getDeviceId()).orElseGet(() -> {
                UserFcmToken created = new UserFcmToken();
                created.setDeviceId(request.getDeviceId());
                created.setCreatedAt(now);
                return created;
            });
            row.setUserId(request.getUserId());
            row.setFcmToken(request.getFcmToken());
            row.setUpdatedAt(now);
            tokenRepository.saveAndFlush(row);   // flush here so a constraint error is reported below

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Device token registered successfully");
            result.put("success", true);
            result.put("userId", request.getUserId());
            return AppResponse.success(REGISTER_API_ID, result, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error registering device token", e);
            throw new ApiException(REGISTER_API_ID, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to register device token: " + messageOf(e));
        }
    }

    @Override
    public AppResponse<Map<String, Object>> removeToken(RemoveTokenRequest body) {
        try {
            tokenRepository.deleteByFcmToken(body.getRequest().getToken());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Device token unregistered successfully");
            result.put("success", true);
            return AppResponse.success(REMOVE_API_ID, result, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error during logout", e);
            throw new ApiException(REMOVE_API_ID, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to logout: " + messageOf(e));
        }
    }

    @Override
    public AppResponse<Map<String, Object>> sendByToken(SendByTokenRequest body) {
        SendByTokenRequest.Payload request = body.getRequest();

        // Accept either one user ("userId") or many ("userIds"), de-duplicated, order preserved.
        Set<String> idSet = new LinkedHashSet<>();
        if (request.getUserIds() != null) {
            request.getUserIds().stream().filter(StringUtils::hasText).forEach(idSet::add);
        }
        if (StringUtils.hasText(request.getUserId())) {
            idSet.add(request.getUserId());
        }
        if (idSet.isEmpty()) {
            throw new ApiException(SEND_API_ID, HttpStatus.BAD_REQUEST, "userId or userIds is required");
        }
        List<String> ids = new ArrayList<>(idSet);

        try {
            List<UserFcmToken> devices = new ArrayList<>();
            for (List<String> chunk : partition(ids, QUERY_IN_CHUNK)) {
                devices.addAll(tokenRepository.findByUserIdIn(chunk));
            }
            List<String> tokens = devices.stream().map(UserFcmToken::getFcmToken).toList();
            Set<String> usersWithDevices = new LinkedHashSet<>();
            devices.forEach(device -> usersWithDevices.add(device.getUserId()));
            List<String> usersWithoutDevices = ids.stream()
                    .filter(id -> !usersWithDevices.contains(id))
                    .toList();

            if (tokens.isEmpty()) {
                throw new ApiException(SEND_API_ID, HttpStatus.NOT_FOUND,
                        "No registered devices found for the given user(s): " + usersWithoutDevices);
            }

            FirebaseMessaging messaging = firebaseMessaging.getObject();
            Map<String, String> data = stringifyData(request.getData());

            int successCount = 0;
            int failureCount = 0;
            List<String> deadTokens = new ArrayList<>();

            for (List<String> batch : partition(tokens, FCM_BATCH_SIZE)) {
                MulticastMessage message = MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(Notification.builder()
                                .setTitle(request.getTitle())
                                .setBody(request.getBody())
                                .build())
                        .putAllData(data)
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setChannelId("default")
                                        .build())
                                .build())
                        .build();

                BatchResponse response = messaging.sendEachForMulticast(message);
                successCount += response.getSuccessCount();
                failureCount += response.getFailureCount();

                // Collect tokens of devices that uninstalled the app or whose token expired.
                List<SendResponse> responses = response.getResponses();
                for (int j = 0; j < responses.size(); j++) {
                    SendResponse r = responses.get(j);
                    if (!r.isSuccessful()
                            && r.getException() != null
                            && DEAD_TOKEN_ERRORS.contains(r.getException().getMessagingErrorCode())) {
                        deadTokens.add(batch.get(j));
                    }
                }
            }

            if (!deadTokens.isEmpty()) {
                for (List<String> chunk : partition(deadTokens, QUERY_IN_CHUNK)) {
                    tokenRepository.deleteByFcmTokenIn(chunk);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("userCount", usersWithDevices.size());
            result.put("deviceCount", tokens.size());
            result.put("successCount", successCount);
            result.put("failureCount", failureCount);
            result.put("removedTokens", deadTokens.size());
            result.put("usersWithoutDevices", usersWithoutDevices);
            return AppResponse.success(SEND_API_ID, result, HttpStatus.OK);
        } catch (ApiException e) {
            throw e;   // the 404 above must not be turned into a 500 by the catch below
        } catch (Exception e) {
            log.error("Send Notification Error", e);
            throw new ApiException(SEND_API_ID, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to send notification: " + messageOf(e));
        }
    }

    // ---------------------------------------------------------------- helpers

    /** FCM data payloads are string-only, so every value is converted with String.valueOf. */
    private static Map<String, String> stringifyData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> out = new LinkedHashMap<>();
        data.forEach((k, v) -> out.put(k, String.valueOf(v)));
        return out;
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }

    private static String messageOf(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : UNKNOWN_ERROR_MSG;
    }
}