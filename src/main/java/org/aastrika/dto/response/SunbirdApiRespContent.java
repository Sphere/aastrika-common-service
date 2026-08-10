package org.aastrika.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SunbirdApiRespContent {

    private String rootOrgName;
    private String channel;
    private String id;

    private String identifier;
    private String rootOrgId;

    private String firstName;
    private String dob;

    private String userType;

    private String lastName;

    private String gender;

    private List<String> roles;

    private String countryCode;

    private String email;

    private String userName;

    private List<SunbirdApiRespOragainsation> organisations;

    private Boolean isMdo;

    private Boolean isCbp;

}

