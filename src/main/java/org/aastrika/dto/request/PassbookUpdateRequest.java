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
 * Add passbook entries for {@code userId}. The acting user (from the {@code x-authenticated-userid}
 * header) is recorded as {@code createdBy} on each entry, not as the owner.
 *
 * <p>Body shape matches the source repo verbatim — the payload sits under a {@code request} key —
 * so existing app callers keep working.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassbookUpdateRequest {

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

        @JsonProperty("typeName")
        @NotBlank
        private String typeName;

        @JsonProperty("competencyDetails")
        @NotEmpty
        @Valid
        private List<CompetencyDetail> competencyDetails;
    }
}
