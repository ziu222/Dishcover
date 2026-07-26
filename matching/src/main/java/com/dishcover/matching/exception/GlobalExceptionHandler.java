package com.dishcover.matching.exception;

import com.dishcover.common.security.PlanRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PlanRequiredException.class)
    ResponseEntity<ApiError> handlePlanRequired(PlanRequiredException ex) {
        return build(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_REQUIRED", ex.getMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message) {
        String traceId = UUID.randomUUID().toString();
        return ResponseEntity.status(status).body(new ApiError(code, message, traceId));
    }
}
