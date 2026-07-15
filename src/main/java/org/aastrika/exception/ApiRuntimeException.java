package org.aastrika.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class ApiRuntimeException extends RuntimeException {
  private String apiId;
  private String message;
  private String error;
  private HttpStatus status;
  private Object result;

  public ApiRuntimeException(HttpStatus httpStatus, String errorMessage, Object result) {
    this.status = httpStatus;
    this.message = errorMessage;
    this.result = result;
  }

  public ApiRuntimeException(HttpStatus httpStatus, String errorMessage) {
    this.status = httpStatus;
    this.message = errorMessage;
  }
}
