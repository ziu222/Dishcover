package com.dishcover.user.exception;

import com.dishcover.common.exception.ApiError;
import com.dishcover.common.exception.CommonExceptionHandler;
import com.dishcover.user.exception.ApiExceptions.CaptchaRequiredException;
import com.dishcover.user.exception.ApiExceptions.EmailAlreadyExistsException;
import com.dishcover.user.exception.ApiExceptions.InvalidCredentialsException;
import com.dishcover.user.exception.ApiExceptions.TooManyAttemptsException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler tập trung của User Service — bắt các exception nghiệp vụ riêng của module
 * này ({@link com.dishcover.user.exception.ApiExceptions}) và chuyển sang mã HTTP + body chuẩn
 * (kế thừa hành vi chung từ {@code CommonExceptionHandler}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends CommonExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ResponseEntity<ApiError> handleConflict(EmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, "EMAIL_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> handleUnauthorized(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage());
    }

    @ExceptionHandler(CaptchaRequiredException.class)
    ResponseEntity<ApiError> handleCaptchaRequired(CaptchaRequiredException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "CAPTCHA_REQUIRED", ex.getMessage());
    }

    /** 429 kèm Retry-After xấp xỉ cửa sổ khoá (không cần tracker lộ thời điểm hết hạn chính xác). */
    @ExceptionHandler(TooManyAttemptsException.class)
    ResponseEntity<ApiError> handleTooManyAttempts(TooManyAttemptsException ex) {
        ResponseEntity<ApiError> base = build(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS", ex.getMessage());
        return ResponseEntity.status(base.getStatusCode())
                .header(HttpHeaders.RETRY_AFTER, "900")
                .body(base.getBody());
    }
}
