package org.example.besmarthelpdesk.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.dto.ResponseGeneral;
import org.example.besmarthelpdesk.enums.ErrorCode;
import org.example.besmarthelpdesk.exception.BaseException;
import org.example.besmarthelpdesk.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final MessageService messageService;

  private String getRequestInputs(HttpServletRequest request) {
    StringBuilder inputs = new StringBuilder();
    inputs.append("URI: ").append(request.getRequestURI());
    String queryString = request.getQueryString();
    if (queryString != null) {
      inputs.append("?").append(queryString);
    }
    Map<String, String[]> parameterMap = request.getParameterMap();
    if (parameterMap != null && !parameterMap.isEmpty()) {
      inputs.append(", Params: {");
      parameterMap.forEach((k, v) -> inputs.append(k).append("=").append(String.join(",", v)).append("; "));
      inputs.append("}");
    }
    return inputs.toString();
  }

  private String getThrowingMethodName(Exception e) {
    if (e.getStackTrace() != null && e.getStackTrace().length > 0) {
      for (StackTraceElement element : e.getStackTrace()) {
        if (element.getClassName().startsWith("org.example")) {
          return element.getMethodName();
        }
      }
      return e.getStackTrace()[0].getMethodName();
    }
    return "UnknownMethod";
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleSpringAccessDeniedException(
      final AccessDeniedException e, HttpServletRequest request) {
    String methodName = getThrowingMethodName(e);
    String inputs = getRequestInputs(request);
    log.warn("({}) {} | Access denied: {}", methodName, inputs, e.getMessage());

    ErrorCode errorCode = ErrorCode.AUTH_FORBIDDEN;
    String localizedMessage = messageService.getMessage(errorCode.getMessage());

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        null
    );

    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleAuthenticationException(
      final AuthenticationException e, HttpServletRequest request) {
    String methodName = getThrowingMethodName(e);
    String inputs = getRequestInputs(request);
    log.warn("({}) {} | Authentication exception: {}", methodName, inputs, e.getMessage());

    ErrorCode errorCode = ErrorCode.AUTH_UNAUTHORIZED;
    String localizedMessage = messageService.getMessage(errorCode.getMessage());

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        null
    );

    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleBaseException(
      final BaseException e, HttpServletRequest request) {
    String methodName = getThrowingMethodName(e);
    String inputs = getRequestInputs(request);
    log.warn("({}) {} | Business exception: {}", methodName, inputs, e.getMessage());

    ErrorCode errorCode = e.getErrorCode();
    String messageKey = (e.getMessage() != null && !e.getMessage().isEmpty()) ? e.getMessage() : errorCode.getMessage();
    String localizedMessage = messageService.getMessage(messageKey, e.getParams());
    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        null
    );

    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleMethodArgumentNotValidException(
      final MethodArgumentNotValidException e, HttpServletRequest request) {
    String methodName = getThrowingMethodName(e);
    String inputs = getRequestInputs(request);
    log.warn("({}) {} | Validation exception: {}", methodName, inputs, e.getMessage());

    Map<String, String> fieldErrors = new HashMap<>();
    List<FieldError> fieldErrorList = e.getBindingResult().getFieldErrors();
    for (FieldError fieldError : fieldErrorList) {
      String localizedFieldMsg = messageService.getMessage(fieldError.getDefaultMessage());
      fieldErrors.put(fieldError.getField(), localizedFieldMsg);
    }

    ErrorCode errorCode = ErrorCode.VALIDATION_INVALID_INPUT;
    String localizedMessage = fieldErrorList.stream()
        .map(fieldError -> messageService.getMessage(fieldError.getDefaultMessage()))
        .collect(Collectors.joining(", "));
    if (localizedMessage.isEmpty()) {
      localizedMessage = messageService.getMessage(errorCode.getMessage());
    }

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        fieldErrors
    );

    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleConstraintViolationException(
      final ConstraintViolationException e, HttpServletRequest request) {
    String methodName = getThrowingMethodName(e);
    String inputs = getRequestInputs(request);
    log.warn("({}) {} | Constraint violation error occurred", methodName, inputs);

    Map<String, String> fieldErrors = new HashMap<>();
    for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
      String propertyPath = violation.getPropertyPath().toString();
      String fieldName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);

      String localizedFieldMsg = messageService.getMessage(violation.getMessage());
      fieldErrors.put(fieldName, localizedFieldMsg);
    }

    ErrorCode errorCode = ErrorCode.VALIDATION_INVALID_INPUT;
    String localizedMessage = e.getConstraintViolations().stream()
        .map(violation -> messageService.getMessage(violation.getMessage()))
        .collect(Collectors.joining(", "));
    if (localizedMessage.isEmpty()) {
      localizedMessage = messageService.getMessage(errorCode.getMessage());
    }

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        fieldErrors
    );
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleHttpMessageNotReadableException(
      final HttpMessageNotReadableException e, HttpServletRequest request) {
    String methodName = getThrowingMethodName(e);
    String inputs = getRequestInputs(request);
    log.warn("({}) {} | Request body is invalid: {}", methodName, inputs, e.getMessage());

    ErrorCode errorCode = ErrorCode.VALIDATION_INVALID_INPUT;
    String localizedMessage = messageService.getMessage(errorCode.getMessage());
    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        null
    );
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ResponseGeneral<Object>> handleGlobalException(
      final Exception e, HttpServletRequest request) {
    String methodName = getThrowingMethodName(e);
    String inputs = getRequestInputs(request);
    log.error("({}) {} | System error encountered: ", methodName, inputs, e);

    ErrorCode errorCode = ErrorCode.SYSTEM_INTERNAL_ERROR;
    String localizedMessage = messageService.getMessage(errorCode.getMessage());

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        null
    );
    return new ResponseEntity<>(response, errorCode.getStatus());
  }
}
