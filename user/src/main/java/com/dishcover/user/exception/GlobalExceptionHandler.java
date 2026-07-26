package com.dishcover.user.exception;

import com.dishcover.common.exception.ApiError;
import com.dishcover.common.exception.CommonExceptionHandler;
import com.dishcover.user.exception.ApiExceptions.EmailAlreadyExistsException;
import com.dishcover.user.exception.ApiExceptions.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
