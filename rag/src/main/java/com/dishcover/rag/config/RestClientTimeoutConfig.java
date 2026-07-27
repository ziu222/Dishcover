package com.dishcover.rag.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Timeout riêng cho 2 client nội bộ (Matching/User) — dùng {@code RestClient.builder()} độc lập,
 * KHÔNG dùng {@code RestClientCustomizer} (global, áp lên MỌI RestClient.Builder trong context,
 * kể cả builder Spring AI dùng nội bộ cho Gemini — phát hiện lúc code-reviewer subagent verify
 * thật: customizer cũ khiến lời gọi LLM cũng bị timeout 5s, dẫn tới fallback gần như luôn luôn).
 * {@link com.dishcover.rag.client.RagMatchingClient}/{@code RagUserClient} nhận builder này qua
 * {@code @Qualifier}; Spring AI tự tạo builder RIÊNG cho Gemini, không bị ảnh hưởng.
 */
@Configuration
public class RestClientTimeoutConfig {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 5000;

    @Bean
    @Qualifier("internalServiceRestClientBuilder")
    RestClient.Builder internalServiceRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return RestClient.builder().requestFactory(factory);
    }
}
