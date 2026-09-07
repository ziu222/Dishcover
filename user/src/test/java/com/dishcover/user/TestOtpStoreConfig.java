package com.dishcover.user;

import com.dishcover.user.security.OtpStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Ghi đè bean {@code OtpStore} trong test bằng cooldown=0 — helper {@code register()} trong
 * {@code AuthFlowIntegrationTest} gọi {@code otpStore.issue()} lần thứ 2 ngay sau khi
 * {@code register()} thật đã gọi lần 1, cooldown 60s thật sẽ ném {@code TooSoonException} và làm
 * vỡ mọi test dùng helper này. TTL vẫn giữ 10 phút thật — không ảnh hưởng gì tới các test khác.
 */
@TestConfiguration
public class TestOtpStoreConfig {

    @Bean
    @Primary
    OtpStore otpStore() {
        return new OtpStore(Duration.ofMinutes(10), Duration.ZERO);
    }
}
