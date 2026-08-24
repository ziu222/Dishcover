package com.dishcover.image.exception;

/**
 * 422 — ảnh gửi lên không hợp lệ (sai định dạng, vượt dung lượng, không giải mã được). Chặn ở
 * {@code ImageValidator}/{@code ImageResizer} TRƯỚC khi tốn tiền gọi Vision API (CLAUDE.md mục 7).
 */
public class InvalidImageException extends RuntimeException {
    public InvalidImageException(String message) {
        super(message);
    }
}
