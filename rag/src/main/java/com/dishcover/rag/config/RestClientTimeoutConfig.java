package com.dishcover.rag.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Timeout cho mọi RestClient trong RAG Service — tránh 1 service treo (không lỗi, chỉ chậm) làm
 * RAG treo theo, circuit breaker không kịp trip vì cuộc gọi chưa "thất bại". Áp qua
 * RestClientCustomizer (Spring Boot tự áp vào RestClient.Builder bean trước khi inject) — KHÔNG
 * sửa constructor từng client, tránh phá MockRestServiceServer đang bind trực tiếp vào
 * RestClient.Builder ở test. Copy nguyên pattern matching/config/RestClientTimeoutConfig.java.
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
