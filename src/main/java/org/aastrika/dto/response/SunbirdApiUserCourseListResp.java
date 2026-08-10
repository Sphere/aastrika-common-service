package org.aastrika.dto.response;


import lombok.Getter;
import lombok.Setter;
import org.aastrika.dto.SunbirdApiUserCourseResult;

@Getter
@Setter
public class SunbirdApiUserCourseListResp {
    private String id;
    private String ver;
    private String ts;
    private SunbirdApiRespParam params;
    private String responseCode;
    private SunbirdApiUserCourseResult result;
}
