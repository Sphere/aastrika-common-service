package org.aastrika.exception;

import org.springframework.http.HttpStatus;

/**
 * Concrete {@link ApiRuntimeException} for signalling an API error with an id, HTTP status and
 * an error code/message. Caught by {@code ApplicationExceptionHandler} and rendered as an
 * {@code AppResponse} error envelope.
 */
public class ApiException extends ApiRuntimeException {

    public ApiException(String apiId, HttpStatus status, String errorMessage) {
        super(status, errorMessage);
        setApiId(apiId);
    }
}
