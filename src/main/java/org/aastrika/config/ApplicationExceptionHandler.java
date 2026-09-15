package org.aastrika.config;

import lombok.extern.slf4j.Slf4j;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.exception.ApiRuntimeException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestControllerAdvice
@Slf4j
public class ApplicationExceptionHandler {

  @ExceptionHandler(ApiRuntimeException.class)
  public ResponseEntity<AppResponse> handleBadRequest(ApiRuntimeException exception) {
    log.warn("{} [{}]: {}", exception.getStatus(), exception.getApiId(), exception.getMessage());
    AppResponse response = AppResponse.error(exception.getApiId(), exception.getMessage(), exception.getStatus());
    if (exception.getResult() != null) {
      response.setResult(exception.getResult());
    }
    return ResponseEntity.status(exception.getStatus()).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<AppResponse> handleValidationException(MethodArgumentNotValidException exception) {
    String errorMessages = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));
    log.warn("Validation failed: {}", errorMessages);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(AppResponse.error(null, errorMessages, HttpStatus.BAD_REQUEST));
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<AppResponse> handleMethodValidationException(HandlerMethodValidationException exception) {
    String errorMessages = Stream.concat(exception.getBeanResults().stream(), exception.getValueResults().stream())
        .flatMap(result -> result.getResolvableErrors().stream())
        .map(error -> error.getDefaultMessage())
        .collect(Collectors.joining(", "));
    log.warn("Validation failed: {}", errorMessages);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(AppResponse.error(null, errorMessages, HttpStatus.BAD_REQUEST));
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<AppResponse> handleMissingHeader(MissingRequestHeaderException exception) {
    log.warn("Missing required header: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(AppResponse.error(null, exception.getMessage(), HttpStatus.BAD_REQUEST));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<AppResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
    // Deliberately do not log the root-cause detail or the exception itself (its stack trace's
    // first line embeds the same text) — a Postgres unique-constraint message can contain the
    // actual conflicting value (e.g. an email), and SECURITY.md 2.5 forbids logging PII.
    log.warn("Data integrity violation");
    String message = "Data integrity violation";
    Throwable rootCause = exception.getRootCause();
    if (rootCause != null) {
      message = rootCause.getMessage();
    }

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(AppResponse.error(null, message, HttpStatus.CONFLICT));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<AppResponse> handleIllegalArgument(IllegalArgumentException exception) {
    log.warn("Bad request: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(AppResponse.error(null, exception.getMessage(), HttpStatus.BAD_REQUEST));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<AppResponse> handleNoResourceFound(NoResourceFoundException exception) {
    log.warn("No resource found: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(AppResponse.error(null, exception.getMessage(), HttpStatus.NOT_FOUND));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<AppResponse> handleGenericException(Exception exception) {
    log.error("Unexpected error occurred: {}", exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(AppResponse.error("api.entity.error", exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
  }
}
