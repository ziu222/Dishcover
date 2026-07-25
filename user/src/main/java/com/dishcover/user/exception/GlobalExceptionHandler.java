package com.dishcover.user.exception;

import com.dishcover.user.exception.ApiExceptions.EmailAlreadyExistsException;
import com.dishcover.user.exception.ApiExceptions.InvalidCredentialsException;
import com.dishcover.user.exception.ApiExceptions.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ResponseEntity<ApiError> handleConflict(EmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, "EMAIL_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> handleUnauthorized(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", detail);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message) {
        String traceId = UUID.randomUUID().toString();
        return ResponseEntity.status(status).body(new ApiError(code, message, traceId));
    }
}
