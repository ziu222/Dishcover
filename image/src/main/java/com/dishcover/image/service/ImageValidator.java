package com.dishcover.image.service;

import com.dishcover.image.exception.InvalidImageException;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Chặn ảnh không hợp lệ TRƯỚC khi tốn tiền gọi Vision API (CLAUDE.md mục 7: jpg/png/webp, ≤5MB).
 * Đây là biên tin cậy — không tin content-type client khai báo một mình, nhưng ở quy mô đồ án chỉ
 * kiểm content-type + dung lượng là đủ (magic-byte sniffing là nâng cấp sau, chưa cần).
 */
@Component
public class ImageValidator {

    /** 5MB — khớp giới hạn multipart ở application.yml (chặn 2 tầng: servlet + nghiệp vụ). */
    static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    /**
     * @param bytes       nội dung ảnh
     * @param contentType content-type client gửi (VD "image/png")
     * @throws InvalidImageException nếu rỗng, sai định dạng, hoặc vượt 5MB
     */
    public void validate(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new InvalidImageException("Ảnh rỗng hoặc không đọc được");
        }
        if (bytes.length > MAX_BYTES) {
            throw new InvalidImageException("Ảnh vượt quá 5MB");
        }
        String normalized = contentType == null ? "" : contentType.toLowerCase().trim();
        if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
            throw new InvalidImageException(
                    "Chỉ hỗ trợ ảnh JPG/PNG/WEBP, nhận được: " + contentType);
        }
    }
}
