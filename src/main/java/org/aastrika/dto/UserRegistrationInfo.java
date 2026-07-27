package org.aastrika.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistrationInfo {
    private String registrationCode;
    private String firstName;
    private String lastName;
    private String email;
    private String position;
    private String source;
    private String orgName;
    private String channel;
    private String organisationType;
    private String organisationSubType;
    private String mapId;
    private String sbRootOrgId;
    private String sbOrgId;
    private String phone;
}

