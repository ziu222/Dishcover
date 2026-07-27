package com.dishcover.rag.llm;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Bọc lời gọi LLM bằng Circuit Breaker + TimeLimiter (CLAUDE.md mục 6 "Fallback bắt buộc" +
 * mục 4.2 mẫu config, instance "llm" — specs/rag-service.md mục 3.5). Resilience thật nằm ở
 * {@link ResilientLlmCaller} (bean riêng — xem Javadoc class đó vì sao không đặt chung ở đây).
 */
@Component
public class LlmGateway {

    private static final long CALL_TIMEOUT_SECONDS = 20; // đệm hơn 15s của TimeLimiter bên dưới

    private final ResilientLlmCaller caller;

    public LlmGateway(ResilientLlmCaller caller) {
        this.caller = caller;
    }

    /** chat() blocking — phần còn lại của service (servlet stack) không phải xử lý Future. */
    public LlmChatResult chat(String prompt) {
        try {
            String answer = caller.chatAsync(prompt).get(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            // Gemini có thể trả generation rỗng (safety block...) -> answer null/blank, KHÔNG
            // ném exception -- phải coi là fallback, không phải "thành công với câu trả lời rỗng".
            if (answer == null || answer.isBlank()) {
                return new LlmChatResult(null, true);
            }
            return new LlmChatResult(answer, false);
        } catch (Exception ex) {
            return new LlmChatResult(null, true);
        }
    }
}
