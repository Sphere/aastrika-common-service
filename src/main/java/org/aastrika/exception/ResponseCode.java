package org.aastrika.exception;

import org.apache.commons.lang3.StringUtils;

public enum ResponseCode {

    unAuthorized(ResponseMessage.Key.UNAUTHORIZED_USER, ResponseMessage.Message.UNAUTHORIZED_USER),
    internalError(ResponseMessage.Key.INTERNAL_ERROR, ResponseMessage.Message.INTERNAL_ERROR),

    OK(200),
    CLIENT_ERROR(400),
    SERVER_ERROR(500);

    private int responseCode;
    private String errorCode;
    private String errorMessage;

    ResponseCode(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    ResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * Returns the matching ResponseCode for the given error code.
     *
     * @param errorCode the error code
     * @return matching ResponseCode or null if not found
     */
    public static ResponseCode getResponse(String errorCode) {
        if (StringUtils.isBlank(errorCode)) {
            return null;
        }

        for (ResponseCode response : ResponseCode.values()) {
            if (errorCode.equalsIgnoreCase(response.getErrorCode())) {
                return response;
            }
        }

        return null;
    }

    public String getMessage(int errorCode) {
        return "";
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }
}
