package com.dishcover.common.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Kiểm tra header {@code X-Internal-Secret} cho các endpoint gọi giữa service.
 *
 * <p>So sánh bằng {@link MessageDigest#isEqual} chứ KHÔNG dùng {@code String.equals}: equals thoát
 * sớm ở byte lệch đầu tiên nên thời gian phản hồi rò rỉ độ dài tiền tố đúng — lỗ hổng timing thật
 * đã phát hiện lúc review Notification Service và sửa ở đó. Gom về đây để endpoint nội bộ mới
 * không phải nhớ lại bài học đó.
 */
public final class InternalSecretGuard {

    private InternalSecretGuard() {
    }

    /**
     * @param expected secret cấu hình ở service
     * @param actual   giá trị client gửi lên (có thể null)
     * @throws ResponseStatusException 401 nếu không khớp
     */
    public static void verify(String expected, String actual) {
        byte[] a = expected == null ? new byte[0] : expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual == null ? new byte[0] : actual.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(a, b)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Internal secret không hợp lệ");
        }
    }
}
