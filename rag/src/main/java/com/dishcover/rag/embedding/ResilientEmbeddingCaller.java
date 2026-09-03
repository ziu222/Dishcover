package com.dishcover.rag.embedding;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Tách riêng khỏi {@link EmbeddingGateway} — cùng lý do {@code ResilientLlmCaller} (xem Javadoc lớp
 * đó): @CircuitBreaker/@TimeLimiter chỉ có hiệu lực qua Spring AOP proxy, self-invocation bỏ qua
 * proxy đó hoàn toàn. Phải gọi CROSS-BEAN từ {@link EmbeddingGateway}.
 */
@Component
public class ResilientEmbeddingCaller {

    private final EmbeddingProvider provider;

    public ResilientEmbeddingCaller(EmbeddingProvider provider) {
        this.provider = provider;
    }

    /**
     * Gọi {@link EmbeddingProvider#embed} bất đồng bộ, bọc Circuit Breaker + TimeLimiter (instance
     * "embedding"). Phải gọi cross-bean (từ {@link EmbeddingGateway}).
     */
    @CircuitBreaker(name = "embedding", fallbackMethod = "fallbackEmbedAsync")
    @TimeLimiter(name = "embedding")
    public CompletableFuture<float[]> embedAsync(String text) {
        return CompletableFuture.supplyAsync(() -> provider.embed(text));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<float[]> fallbackEmbedAsync(String text, Throwable ex) {
        return CompletableFuture.failedFuture(ex);
    }
}
