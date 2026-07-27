package com.dishcover.rag.llm;

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
 * không gọi Gemini thật (specs/rag-service.md mục 3.5/6).
 */
@SpringBootTest
@ActiveProfiles("test")
class LlmGatewayTest {

    @Autowired
    LlmGateway llmGateway;
    @MockitoBean
    LlmProvider llmProvider;

    @Test
    void providerFailureFallsBackWithoutCrashing() {
        when(llmProvider.complete(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("Gemini API lỗi giả lập"));

        LlmChatResult result = llmGateway.chat("câu hỏi bất kỳ");

        assertTrue(result.usedFallback());
        assertNull(result.answer());
    }

    @Test
    void providerSuccessReturnsAnswerWithoutFallback() {
        when(llmProvider.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn("Câu trả lời thật");

        LlmChatResult result = llmGateway.chat("câu hỏi bất kỳ");

        assertTrue(!result.usedFallback());
        assertTrue(result.answer().contains("Câu trả lời thật"));
    }
}
