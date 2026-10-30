package org.aastrika.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterTokenRequest {

    @JsonProperty("request")
    @NotNull
    @Valid
    private Payload request;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        @JsonProperty("userId")
        @NotBlank(message = "request.userId is required")
        private String userId;

        @JsonProperty("fcmToken")
        @NotBlank(message = "request.fcmToken is required")
        private String fcmToken;

        @JsonProperty("deviceId")
        @NotBlank(message = "request.deviceId is required")
        private String deviceId;

        @JsonProperty("status")
        private String status;
    }
}
