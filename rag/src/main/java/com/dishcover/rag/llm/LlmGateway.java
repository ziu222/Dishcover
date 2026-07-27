package com.dishcover.rag.llm;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Bọc lời gọi LLM bằng Circuit Breaker + TimeLimiter (CLAUDE.md mục 6 "Fallback bắt buộc" +
 * mục 4.2 mẫu config, instance "llm" — specs/rag-service.md mục 3.5).
 */
@Component
public class LlmGateway {

    private static final long CALL_TIMEOUT_SECONDS = 20; // đệm hơn 15s của TimeLimiter bên dưới

    private final LlmProvider provider;

    public LlmGateway(LlmProvider provider) {
        this.provider = provider;
    }

    /** chat() blocking — phần còn lại của service (servlet stack) không phải xử lý Future. */
    public LlmChatResult chat(String prompt) {
        try {
            String answer = chatAsync(prompt).get(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return new LlmChatResult(answer, false);
        } catch (Exception ex) {
            return new LlmChatResult(null, true);
        }
    }

    @CircuitBreaker(name = "llm", fallbackMethod = "fallbackChatAsync")
    @TimeLimiter(name = "llm")
    CompletableFuture<String> chatAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> provider.complete(prompt));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<String> fallbackChatAsync(String prompt, Throwable ex) {
        return CompletableFuture.failedFuture(ex); // chat() bên ngoài bắt đồng nhất -> usedFallback=true
    }
}
