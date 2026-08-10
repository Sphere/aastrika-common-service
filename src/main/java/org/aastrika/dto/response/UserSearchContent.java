package org.aastrika.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A trimmed view of the user-search service's {@code result.response.content[]} entry — only the
 * fields the cohort endpoints read. Mirrors the source {@code SearchUserApiContent} (camelCase JSON).
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSearchContent {

    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String channel;
    private ProfileDetails profileDetails;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfileDetails {
        private List<ProfessionalDetail> professionalDetails;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfessionalDetail {
        private String designation;
        private String designationOther;
    }
}
