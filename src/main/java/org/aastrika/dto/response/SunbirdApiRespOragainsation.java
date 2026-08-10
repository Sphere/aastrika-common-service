package org.aastrika.dto.response;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SunbirdApiRespOragainsation {

    private String organisationId;
    private List<String> roles;
    private String userId;
    private String parentOrgId;
    private String id;

}

