package com.dishcover.recipe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Timeout riêng cho client nội bộ (RAG/Matching, Giai đoạn B index) — cùng pattern
 * {@code rag/config/RestClientTimeoutConfig} (connect 3s/read 5s; embed/index bản thân đã có
 * TimeLimiter 15-20s riêng ở phía RAG, đây chỉ là timeout tầng HTTP thô).
 */
@Configuration
public class RestClientTimeoutConfig {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 20000; // đủ dư cho TimeLimiter 15-20s bên phía RAG embed

    @Bean
    RestClient.Builder internalServiceRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return RestClient.builder().requestFactory(factory);
    }
}
