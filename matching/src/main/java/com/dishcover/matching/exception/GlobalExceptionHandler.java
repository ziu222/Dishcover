package com.dishcover.matching.exception;

import com.dishcover.common.exception.ApiError;
import com.dishcover.common.exception.CommonExceptionHandler;
import com.dishcover.matching.exception.ApiExceptions.UpstreamUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** ResourceNotFoundException/validation kế thừa từ CommonExceptionHandler (common). */
@RestControllerAdvice
public class GlobalExceptionHandler extends CommonExceptionHandler {

    @ExceptionHandler(UpstreamUnavailableException.class)
    ResponseEntity<ApiError> handleUpstreamUnavailable(UpstreamUnavailableException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", ex.getMessage());
    }
}
