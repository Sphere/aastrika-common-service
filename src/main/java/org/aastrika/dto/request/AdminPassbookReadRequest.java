package org.aastrika.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin read of one or more users' passbooks. Unlike {@link PassbookReadRequest}, the acting
 * user's header is ignored and the target users come from the body.
 *
 * <p>The source repo used the key {@code userId} for this list; it is exposed here as
 * {@code userIds} for clarity. Change the {@code @JsonProperty} if a client contract requires
 * the original name.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPassbookReadRequest {

    @JsonProperty("typeName")
    @NotBlank
    private String typeName;

    @JsonProperty("userIds")
    @NotEmpty
    private List<String> userIds;
}
