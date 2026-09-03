package com.dishcover.rag.embedding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * KHÔNG gọi OpenAI thật (nhất quán với {@code OpenAiProviderTest} bên llm) — gọi thật tốn tiền và
 * phụ thuộc khóa, chỉ làm ở bước live-verify thủ công.
 */
class OpenAiEmbeddingProviderTest {

    @Test
    void constructsWithoutCallingOpenAi() {
        OpenAiEmbeddingProvider provider =
                new OpenAiEmbeddingProvider("fake-key", "https://api.openai.com", "text-embedding-3-small");

        assertNotNull(provider);
    }
}
