package com.dishcover.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    /**
     * Body không đọc được: JSON sai cú pháp, sai encoding (byte không phải UTF-8), hoặc kiểu
     * không parse được (VD ngày "31-12-2026"). Trước khi có fix ERROR-dispatch, các lỗi này bị
     * trả 401 gây hiểu nhầm "hết phiên". KHÔNG lộ chi tiết nội bộ trong message.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Nội dung request không hợp lệ (JSON, ngày, hoặc mã hoá ký tự sai).");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException ex) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_HEADER",
                "Thiếu header bắt buộc: " + ex.getHeaderName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_PARAMETER",
                "Tham số không hợp lệ: " + ex.getName());
    }

    protected ResponseEntity<ApiError> build(HttpStatus status, String code, String message) {
        String traceId = UUID.randomUUID().toString();
        return ResponseEntity.status(status).body(new ApiError(code, message, traceId));
    }
}
