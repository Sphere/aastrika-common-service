package org.aastrika.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin read of one or more users' passbooks. Unlike {@link PassbookReadRequest}, the acting
 * user's header is ignored and the target users come from the body.
 *
 * <p>Body shape matches the source repo verbatim, including its {@code userId} key for what is
 * actually a list of users. The Java field stays {@code userIds} for readability; the wire name
 * is pinned by {@code @JsonProperty} because the app integration depends on it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPassbookReadRequest {

    @JsonProperty("request")
    @NotNull
    @Valid
    private Payload request;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        @JsonProperty("typeName")
        @NotBlank
        private String typeName;

        @JsonProperty("userId")
        @NotEmpty
        private List<String> userIds;
    }
}
