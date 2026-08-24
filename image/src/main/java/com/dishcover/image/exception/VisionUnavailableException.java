package com.dishcover.image.exception;

/**
 * 503 — Vision API lỗi/timeout/mạch ngắt (circuit breaker mở). Map sang thông báo "Nhận diện ảnh
 * tạm thời không khả dụng, bạn có thể nhập tay" để UI chuyển form nhập tay (CLAUDE.md mục 7
 * "Fallback" + mục 3 "không màn hình lỗi trắng").
 */
public class VisionUnavailableException extends RuntimeException {
    public VisionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
