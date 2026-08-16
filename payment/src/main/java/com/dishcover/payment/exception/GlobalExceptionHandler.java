package com.dishcover.payment.exception;

import com.dishcover.common.exception.ApiError;
import com.dishcover.common.exception.CommonExceptionHandler;
import com.dishcover.payment.exception.ApiExceptions.InvalidPaymentCallbackException;
import com.dishcover.payment.exception.ApiExceptions.PaymentGatewayUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** PlanRequiredException/ResourceNotFoundException/validation kế thừa từ CommonExceptionHandler (common). */
@RestControllerAdvice
public class GlobalExceptionHandler extends CommonExceptionHandler {

    @ExceptionHandler(InvalidPaymentCallbackException.class)
    ResponseEntity<ApiError> handleInvalidCallback(InvalidPaymentCallbackException ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_CALLBACK", ex.getMessage());
    }

    @ExceptionHandler(PaymentGatewayUnavailableException.class)
    ResponseEntity<ApiError> handleGatewayUnavailable(PaymentGatewayUnavailableException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", ex.getMessage());
    }
}
