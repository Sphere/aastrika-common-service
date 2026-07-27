package org.aastrika.dto.response;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SBApiResponse {

    private String id;
    private String ver;
    private String ts;
    private SunbirdApiRespParam params;
    private HttpStatus responseCode;

    private transient Map<String, Object> response = new HashMap<>();

    public SBApiResponse() {
        this.ver = "v1";
        this.ts = new Timestamp(System.currentTimeMillis()).toString();
        this.params = new SunbirdApiRespParam();
    }

    public SBApiResponse(String id) {
        this();
        this.id = id;
    }
}

