package org.aastrika.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SunbirdApiResultResponse {

    private int count;
    private List<SunbirdApiRespContent> content;

}
