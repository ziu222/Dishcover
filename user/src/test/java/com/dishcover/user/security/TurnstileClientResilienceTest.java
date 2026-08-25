package com.dishcover.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test circuit breaker/fallback THẬT qua Spring AOP proxy (cùng pattern
 * matching.client.ClientResilienceTest) — trỏ turnstile.verify-url vào cổng không ai lắng nghe
 * để ép lỗi kết nối thật, xác nhận đúng thiết kế fail-open đã chốt: Turnstile sập không được
 * chặn đăng nhập, chỉ mất tác dụng CAPTCHA tạm thời.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "turnstile.verify-url=http://localhost:1/turnstile/v0/siteverify")
class TurnstileClientResilienceTest {

    @Autowired
    TurnstileClient turnstileClient;

    @Test
    void unreachableTurnstileFailsOpen() {
        assertTrue(turnstileClient.verify("bat-ky-token-nao", "1.2.3.4"));
    }

    @Test
    void blankTokenFailsClosedWithoutCallingNetwork() {
        // Thiếu token không phải sự cố Turnstile — không nên fail-open, kể cả khi Turnstile sập.
        assertFalse(turnstileClient.verify("", "1.2.3.4"));
        assertFalse(turnstileClient.verify(null, "1.2.3.4"));
    }
}
