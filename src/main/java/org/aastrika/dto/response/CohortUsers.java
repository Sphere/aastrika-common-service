package org.aastrika.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A cohort member as returned by the active-users cohort endpoint. Field/JSON
 * names are kept identical to the source {@code CohortUsers} model (snake_case) so the wire contract
 * is unchanged; all fields are serialized (nulls included) to match the source output exactly.
 */
@Data
@NoArgsConstructor
public class CohortUsers {

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String email;

    private String desc;

    @JsonProperty("user_id")
    private String userId;

    private String department;

    @JsonProperty("phone_No")
    private String phoneNo;

    private String designation;

    private String userLocation;

    private String city;
}
