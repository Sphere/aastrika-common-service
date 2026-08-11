package org.aastrika.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SunbirdApiResp {
    private String id;
    private String ver;
    private String ts;
    private SunbirdApiRespParam params;
    private String responseCode;
    private SunbirdApiRespResult result;
}

