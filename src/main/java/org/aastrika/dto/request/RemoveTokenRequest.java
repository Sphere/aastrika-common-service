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
public class RemoveTokenRequest {

    @JsonProperty("request")
    @NotNull
    @Valid
    private Payload request;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        @JsonProperty("token")
        @NotBlank(message = "request.token is required")
        private String token;
    }
}