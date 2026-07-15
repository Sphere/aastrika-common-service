package org.aastrika.dto.response;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppResponse<T> {
    private String id;
    private String ver;
    private OffsetDateTime ts;
    private Params params;
    private String responseCode;
    private T result;

    public static <T> AppResponse<T> success(String apiId, T result, HttpStatus status) {
        AppResponse<T> response = new AppResponse<>();
        response.setId(apiId);
        response.setVer("v1");
        response.setTs(OffsetDateTime.now(ZoneId.of("Asia/Kolkata")));
        response.setResponseCode(status.getReasonPhrase());
        response.setResult(result);
        Params params = new Params();
        params.setStatus(status.getReasonPhrase());
        response.setParams(params);
        return response;
    }

    public static <T> AppResponse<T> error(String apiId, String errorMessage, HttpStatus status) {
        AppResponse<T> response = new AppResponse<>();
        response.setId(apiId);
        response.setVer("v1");
        response.setTs(OffsetDateTime.now(ZoneId.of("Asia/Kolkata")));
        response.setResponseCode(status.getReasonPhrase());
        Params params = new Params();
        params.setErrmsg(errorMessage);
        params.setStatus(status.getReasonPhrase());
        response.setParams(params);
        return response;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Params {
        private String resmsgid;
        private String msgid;
        private String err;
        private String status;
        private String errmsg;
    }
}
