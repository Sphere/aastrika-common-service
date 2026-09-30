package org.aastrika.dto.request;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for {@code POST /v2/user/assessment/submit}. The {@code questions} carry each option's id,
 * the user's selection ({@code userSelected}/{@code response}) and the correct-answer marker
 * ({@code isCorrect}) — i.e. the answer key rides in the request, so scoring is done in-memory.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSubmitRequest {

    @JsonProperty("timeLimit")
    @NotNull
    private Long timeLimit;

    @JsonProperty("isAssessment")
    @NotNull
    private Boolean isAssessment;

    @JsonProperty("questions")
    @NotEmpty
    private List<Map<String, Object>> questions;

    @JsonProperty("identifier")
    @NotBlank
    private String identifier;

    @JsonProperty("title")
    @NotBlank
    private String title;
}
