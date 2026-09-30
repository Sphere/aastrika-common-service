package org.aastrika.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin org-migration request for {@code PATCH /user/v1/migrate}.
 *
 * <p>Unlike the passbook/ratings migrations, the source's {@code {"request": {...}}} envelope is
 * <b>kept</b> here: this endpoint is called by admin tooling rather than by our own clients, so the
 * wire contract must stay byte-compatible. The source validated the envelope by hand
 * ({@code validateMigrateRequest}); that is expressed as bean validation instead, which also turns
 * the source's 500-on-missing-params into a proper 400.
 *
 * <p>{@code channel} is the {@code channel} of a <b>tenant</b> organisation
 * ({@code sunbird.organisation.istenant = true}). A non-tenant value is rejected downstream by the
 * learner service, not here — we deliberately do not duplicate that lookup.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMigrateRequest {

    @JsonProperty("request")
    @NotNull
    @Valid
    private Payload request;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        @JsonProperty("userId")
        @NotBlank
        private String userId;

        @JsonProperty("channel")
        @NotBlank
        private String channel;
    }
}
