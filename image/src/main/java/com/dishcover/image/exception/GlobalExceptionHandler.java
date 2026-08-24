package com.dishcover.image.exception;

import com.dishcover.common.exception.ApiError;
import com.dishcover.common.exception.CommonExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Kế thừa xử lý lỗi chung (common) + 2 lỗi riêng của Image Service: ảnh không hợp lệ (422) và
 * Vision API không khả dụng (503).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends CommonExceptionHandler {

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ApiError> handleInvalidImage(InvalidImageException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_IMAGE", ex.getMessage());
    }

    @ExceptionHandler(VisionUnavailableException.class)
    public ResponseEntity<ApiError> handleVisionUnavailable(VisionUnavailableException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "VISION_UNAVAILABLE", ex.getMessage());
    }
}
