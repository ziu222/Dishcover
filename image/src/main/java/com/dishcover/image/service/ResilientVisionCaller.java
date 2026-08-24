package com.dishcover.image.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Bọc {@link VisionClient} bằng Circuit Breaker + TimeLimiter (instance "vision", cấu hình
 * application.yml). Tách thành bean RIÊNG — {@code @CircuitBreaker}/{@code @TimeLimiter} chỉ có
 * hiệu lực qua Spring AOP proxy, self-invocation trong cùng bean bỏ qua proxy hoàn toàn (bài học
 * đã verify thật ở {@code rag/llm/ResilientLlmCaller.java}). Orchestrator gọi CROSS-BEAN để đi
 * đúng qua proxy.
 */
@Component
public class ResilientVisionCaller {

    private final VisionClient visionClient;

    public ResilientVisionCaller(VisionClient visionClient) {
        this.visionClient = visionClient;
    }

    /**
     * Gọi Vision API bất đồng bộ, bọc Circuit Breaker + TimeLimiter. Phải được gọi cross-bean để
     * đi qua AOP proxy.
     *
     * @param image ảnh đã resize
     * @return future chứa danh sách nguyên liệu thô; hoàn thành exceptionally nếu mạch mở/timeout/lỗi
     */
    @CircuitBreaker(name = "vision", fallbackMethod = "fallbackRecognizeAsync")
    @TimeLimiter(name = "vision")
    public CompletableFuture<List<RawRecognizedItem>> recognizeAsync(ImageResizer.ResizedImage image) {
        return CompletableFuture.supplyAsync(() -> visionClient.recognize(image));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<List<RawRecognizedItem>> fallbackRecognizeAsync(
            ImageResizer.ResizedImage image, Throwable ex) {
        return CompletableFuture.failedFuture(ex);
    }
}
