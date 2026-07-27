package com.dishcover.rag.llm;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Tách riêng khỏi {@link LlmGateway} — @CircuitBreaker/@TimeLimiter chỉ có hiệu lực qua Spring AOP
 * proxy, mà self-invocation (LlmGateway gọi method của chính nó qua {@code this}) BỎ QUA proxy đó
 * hoàn toàn. Đặt ở 1 bean riêng để LlmGateway gọi CROSS-BEAN, đi đúng qua proxy (phát hiện lúc
 * code-reviewer subagent verify thật bằng probe test — circuit breaker ghi nhận 0 cuộc gọi dù
 * provider luôn throw).
 */
@Component
public class ResilientLlmCaller {

    private final LlmProvider provider;

    public ResilientLlmCaller(LlmProvider provider) {
        this.provider = provider;
    }

    @CircuitBreaker(name = "llm", fallbackMethod = "fallbackChatAsync")
    @TimeLimiter(name = "llm")
    public CompletableFuture<String> chatAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> provider.complete(prompt));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<String> fallbackChatAsync(String prompt, Throwable ex) {
        return CompletableFuture.failedFuture(ex);
    }
}
