package com.dishcover.user.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Timeout cho RestClient dùng gọi Cloudflare Turnstile — tránh Turnstile treo (không lỗi, chỉ
 * chậm) làm login treo theo, circuit breaker không kịp trip vì cuộc gọi chưa "thất bại". Cùng
 * pattern RestClientTimeoutConfig của Matching Service.
 */
@Configuration
public class RestClientTimeoutConfig {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 5000;

    @Bean
    RestClientCustomizer restClientTimeoutCustomizer() {
        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
            factory.setReadTimeout(READ_TIMEOUT_MS);
            builder.requestFactory(factory);
        };
    }
}
