package org.aastrika.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Read a single user's passbook. The user is taken from the {@code x-authenticated-userid}
 * header, so the payload only carries the {@code typeName} (e.g. {@code competency}).
 *
 * <p>Body shape matches the source repo verbatim — {@code {"request": {"typeName": ...}}} — so
 * existing app callers keep working. The envelope is typed and validated here rather than an
 * untyped map (same pattern as {@link UserMigrateRequest}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassbookReadRequest {

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
    }
}