package org.aastrika.service.impl;

import com.google.firebase.messaging.*;

import java.time.OffsetDateTime;
import java.util.*;

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
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationServiceImpl.class);

    private static final Set<MessagingErrorCode> DEAD_TOKEN_ERRORS = Set.of(
            MessagingErrorCode.UNREGISTERED,        // messaging/registration-token-not-registered
            MessagingErrorCode.INVALID_ARGUMENT);   // messaging/invalid-registration-token

    private final UserFcmTokenRepository tokenRepository;
    private final ObjectProvider<FirebaseMessaging> firebaseMessaging;
    private final String unknownErrorMsg;
    private final int fcmBatchSize;
    private final int queryInChunk;
    private static final ObjectMapper JSON = new ObjectMapper();

    public PushNotificationServiceImpl(UserFcmTokenRepository tokenRepository,
                                       ObjectProvider<FirebaseMessaging> firebaseMessaging,
                                       @Value("${notification.unknown-error-msg:Unknown error}") String unknownErrorMsg,
                                       @Value("${notification.fcm-batch-size:500}") int fcmBatchSize,
                                       @Value("${notification.query-in-chunk:1000}") int queryInChunk) {
        this.tokenRepository = tokenRepository;
        this.firebaseMessaging = firebaseMessaging;
        this.unknownErrorMsg = unknownErrorMsg;
        this.fcmBatchSize = fcmBatchSize;
        this.queryInChunk = queryInChunk;
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

        List<String> ids = collectUserIds(request);
        if (ids.isEmpty()) {
            throw new ApiException(SEND_API_ID, HttpStatus.BAD_REQUEST, "userId or userIds is required");
        }
        FirebaseMessaging messaging = firebaseMessaging.getIfAvailable();
        if (messaging == null) {
            throw new ApiException(SEND_API_ID, HttpStatus.SERVICE_UNAVAILABLE, "Firebase is not configured");
        }

        try {
            List<UserFcmToken> devices = findDevices(ids);
            List<String> tokens = devices.stream()
                    .map(UserFcmToken::getFcmToken)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
            Set<String> usersWithDevices = new LinkedHashSet<>();
            devices.forEach(device -> usersWithDevices.add(device.getUserId()));
            List<String> usersWithoutDevices = ids.stream()
                    .filter(id -> !usersWithDevices.contains(id))
                    .toList();

            if (tokens.isEmpty()) {
                throw new ApiException(SEND_API_ID, HttpStatus.NOT_FOUND,
                        "No registered devices found for the given user(s): " + usersWithoutDevices);
            }

            Map<String, String> data = stringifyData(request.getData());
            int successCount = 0;
            int failureCount = 0;
            List<String> deadTokens = new ArrayList<>();

            for (List<String> batch : partition(tokens, fcmBatchSize)) {
                try {
                    BatchResponse response = messaging.sendEachForMulticast(buildMessage(request, data, batch));
                    successCount += response.getSuccessCount();
                    failureCount += response.getFailureCount();
                    collectDeadTokens(batch, response.getResponses(), deadTokens);
                } catch (Exception e) {
                    // One failed batch must not abort the batches already sent or still to send.
                    failureCount += batch.size();
                    log.error("FCM send failed for a batch of {} token(s)", batch.size(), e);
                }
            }

            for (List<String> chunk : partition(deadTokens, queryInChunk)) {
                tokenRepository.deleteByFcmTokenIn(chunk);
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
            throw e;
        } catch (Exception e) {
            log.error("Send Notification Error", e);
            throw new ApiException(SEND_API_ID, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to send notification: " + messageOf(e));
        }
    }

    /** One user ("userId") or many ("userIds"), blanks dropped, de-duplicated, order preserved. */
    private static List<String> collectUserIds(SendByTokenRequest.Payload request) {
        Set<String> ids = new LinkedHashSet<>();
        if (request.getUserIds() != null) {
            request.getUserIds().stream().filter(StringUtils::hasText).forEach(ids::add);
        }
        if (StringUtils.hasText(request.getUserId())) {
            ids.add(request.getUserId());
        }
        return new ArrayList<>(ids);
    }

    private List<UserFcmToken> findDevices(List<String> ids) {
        List<UserFcmToken> devices = new ArrayList<>();
        for (List<String> chunk : partition(ids, queryInChunk)) {
            devices.addAll(tokenRepository.findByUserIdIn(chunk));
        }
        return devices;
    }

    private static MulticastMessage buildMessage(SendByTokenRequest.Payload request,
                                                 Map<String, String> data, List<String> tokens) {
        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getBody())
                        .build())
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder().setChannelId("default").build())
                        .build())
                .build();
    }

    /** Collects tokens FCM reports as unregistered/invalid; null-safe on the error code. */
    private static void collectDeadTokens(List<String> batch, List<SendResponse> responses,
                                          List<String> deadTokens) {
        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (r.isSuccessful()) {
                continue;
            }
            FirebaseMessagingException ex = r.getException();
            MessagingErrorCode code = ex != null ? ex.getMessagingErrorCode() : null;
            if (code != null && DEAD_TOKEN_ERRORS.contains(code)) {
                deadTokens.add(batch.get(i));
            } else {
                log.warn("FCM send failed for one token: code={}, error={}", code,
                        ex != null ? ex.getMessage() : "unknown");
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    /**
     * FCM data payloads are string-only. Strings pass through as-is; objects/arrays (e.g. actionData)
     * are serialised to a JSON string; other scalars use String.valueOf.
     */    private static Map<String, String> stringifyData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : data.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Map || v instanceof Collection) {
                try {
                    out.put(e.getKey(), JSON.writeValueAsString(v));
                } catch (JsonProcessingException ex) {
                    throw new ApiException(SEND_API_ID, HttpStatus.BAD_REQUEST,
                            "Invalid JSON in data field: " + e.getKey());
                }
            } else {
                out.put(e.getKey(), String.valueOf(v));
            }
        }
        return out;
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }

    private String messageOf(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : unknownErrorMsg;
    }
}