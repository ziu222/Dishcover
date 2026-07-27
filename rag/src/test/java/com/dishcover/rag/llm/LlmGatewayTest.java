package com.dishcover.rag.llm;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test circuit breaker/fallback THẬT qua Spring AOP proxy — @MockitoBean LlmProvider để ép lỗi mà
 * không gọi Gemini thật (specs/rag-service.md mục 3.5/6). Assert luôn số liệu
 * CircuitBreakerRegistry — không chỉ triệu chứng (usedFallback), vì self-invocation từng khiến
 * @CircuitBreaker/@TimeLimiter bị BỎ QUA hoàn toàn (0 cuộc gọi ghi nhận) mà LlmChatResult vẫn
 * "đúng" một cách tình cờ (do try/catch ở tầng ngoài bắt được exception thô, không qua aspect nào
 * cả) — phát hiện lúc code-reviewer subagent verify thật, xem ResilientLlmCaller.
 */
@SpringBootTest
@ActiveProfiles("test")
class LlmGatewayTest {

    @Autowired
    LlmGateway llmGateway;
    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;
    @MockitoBean
    LlmProvider llmProvider;

    @Test
    void providerFailureFallsBackWithoutCrashing() {
        when(llmProvider.complete(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("Gemini API lỗi giả lập"));

        LlmChatResult result = llmGateway.chat("câu hỏi bất kỳ");

        assertTrue(result.usedFallback());
        assertNull(result.answer());
        // Chứng minh @CircuitBreaker THỰC SỰ chạy (đi qua proxy) chứ không phải try/catch ở
        // LlmGateway.chat() ngẫu nhiên bắt trúng exception mà bỏ qua toàn bộ resilience layer.
        assertTrue(circuitBreakerRegistry.circuitBreaker("llm").getMetrics().getNumberOfBufferedCalls() > 0);
    }

    @Test
    void providerSuccessReturnsAnswerWithoutFallback() {
        when(llmProvider.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn("Câu trả lời thật");

        LlmChatResult result = llmGateway.chat("câu hỏi bất kỳ");

        assertTrue(!result.usedFallback());
        assertTrue(result.answer().contains("Câu trả lời thật"));
    }
}
