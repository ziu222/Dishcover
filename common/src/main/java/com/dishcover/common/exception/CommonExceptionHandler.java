package com.dishcover.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Base xử lý lỗi dùng chung 4 service (trước đây copy-paste riêng từng service — CLAUDE.md mục 9
 * review đã gộp lại). KHÔNG tự nó là {@code @RestControllerAdvice}: mỗi service khai báo 1
 * subclass (rỗng nếu không có exception riêng) trong package của chính mình để Spring
 * component-scan thấy được — {@code common} không nằm trong base package của service nào, cùng
 * lý do JwtService/JwtAuthFilter phải đăng ký thủ công. Kế thừa (không phải
 * @Bean thủ công) vì @ExceptionHandler là method, method kế thừa Spring vẫn nhận diện được ở
 * subclass — đơn giản hơn.
 */
public class CommonExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", detail);
    }

    protected ResponseEntity<ApiError> build(HttpStatus status, String code, String message) {
        String traceId = UUID.randomUUID().toString();
        return ResponseEntity.status(status).body(new ApiError(code, message, traceId));
    }
}
