package com.dishcover.rag.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * KHÔNG gọi OpenAI thật (nhất quán với {@link GeminiProviderTest}/{@link GroqProviderTest}) —
 * gọi thật tốn tiền và phụ thuộc khóa, chỉ làm ở bước live-verify thủ công.
 */
class OpenAiProviderTest {

    @Test
    void constructsWithoutCallingOpenAi() {
        OpenAiProvider provider = new OpenAiProvider("fake-key", "https://api.openai.com", "gpt-4o-mini");

        assertNotNull(provider);
    }
}
