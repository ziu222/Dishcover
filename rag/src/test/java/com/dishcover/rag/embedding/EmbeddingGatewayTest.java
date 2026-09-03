package com.dishcover.rag.embedding;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test circuit breaker THẬT qua Spring AOP proxy — cùng lý do/pattern {@code LlmGatewayTest}: assert
 * số liệu CircuitBreakerRegistry, không chỉ triệu chứng (Optional rỗng), để chắc @CircuitBreaker/
 * @TimeLimiter không bị self-invocation bỏ qua (xem ResilientEmbeddingCaller).
 */
@SpringBootTest
@ActiveProfiles("test")
class EmbeddingGatewayTest {

    @Autowired
    EmbeddingGateway embeddingGateway;
    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;
    @MockitoBean
    EmbeddingProvider embeddingProvider;

    @Test
    void providerFailureFallsBackToEmptyWithoutCrashing() {
        when(embeddingProvider.embed(anyString())).thenThrow(new RuntimeException("OpenAI lỗi giả lập"));

        Optional<float[]> result = embeddingGateway.embed("câu hỏi bất kỳ");

        assertTrue(result.isEmpty());
        assertTrue(circuitBreakerRegistry.circuitBreaker("embedding").getMetrics().getNumberOfBufferedCalls() > 0);
    }

    @Test
    void providerSuccessReturnsEmbedding() {
        float[] vector = {0.1f, 0.2f, 0.3f};
        when(embeddingProvider.embed(anyString())).thenReturn(vector);

        Optional<float[]> result = embeddingGateway.embed("câu hỏi bất kỳ");

        assertTrue(result.isPresent());
        assertArrayEquals(vector, result.get());
    }
}
