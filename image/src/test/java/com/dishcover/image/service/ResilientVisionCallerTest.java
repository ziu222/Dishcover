package com.dishcover.image.service;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Xác minh @CircuitBreaker/@TimeLimiter trên {@link ResilientVisionCaller} THỰC SỰ weave qua Spring
 * AOP proxy — assert số liệu CircuitBreakerRegistry, không chỉ triệu chứng. Regression guard cho lỗi
 * thiếu aspectjweaver (service không dùng JPA nên không kéo transitively): thiếu nó thì annotation
 * lặng lẽ no-op, buffered call = 0 (VisionClient mock để không gọi Vision API thật).
 */
@SpringBootTest
@ActiveProfiles("test")
class ResilientVisionCallerTest {

    @Autowired
    ResilientVisionCaller caller;
    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;
    @MockitoBean
    VisionClient visionClient;

    @Test
    void visionFailureIsRecordedByCircuitBreaker() {
        when(visionClient.recognize(any())).thenThrow(new RuntimeException("Vision API lỗi giả lập"));

        var future = caller.recognizeAsync(new ImageResizer.ResizedImage(new byte[]{1}, "image/jpeg"));

        assertThrows(ExecutionException.class, future::get);
        // Chứng minh aspect chạy (đi qua proxy) — nếu thiếu aspectjweaver, buffered call = 0.
        assertTrue(circuitBreakerRegistry.circuitBreaker("vision")
                .getMetrics().getNumberOfBufferedCalls() > 0);
    }

    @Test
    void visionSuccessReturnsItemsThroughProxy() throws Exception {
        when(visionClient.recognize(any()))
                .thenReturn(List.of(new RawRecognizedItem("Trứng gà", 0.9, "2 quả")));

        var future = caller.recognizeAsync(new ImageResizer.ResizedImage(new byte[]{1}, "image/jpeg"));

        assertTrue(future.get().size() == 1);
    }
}
