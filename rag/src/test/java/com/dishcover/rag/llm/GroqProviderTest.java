package com.dishcover.rag.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * KHÔNG gọi Groq thật (nhất quán với {@link GeminiProviderTest}). Constructor tự dựng
 * OpenAiApi/OpenAiChatModel nên không mock được ChatModel qua đường ChatClient.Builder như
 * GeminiProviderTest — test này chỉ xác nhận provider khởi tạo được với config hợp lệ (không ném
 * exception) và chatClient nội bộ không null; round-trip thật được cover ở
 * ClientResilienceTest/live-verify thủ công (CLAUDE.md mục 10.7).
 */
class GroqProviderTest {

    @Test
    void constructsWithoutCallingGroq() {
        GroqProvider provider = new GroqProvider("fake-key", "https://api.groq.com/openai", "llama-3.3-70b-versatile");

        assertNotNull(provider);
    }
}
