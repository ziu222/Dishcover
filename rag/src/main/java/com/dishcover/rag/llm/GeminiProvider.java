package com.dishcover.rag.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Gọi Gemini qua endpoint tương thích OpenAI (spring-ai-starter-model-openai, cấu hình ở
 * application.yml, specs/rag-service.md mục 3.5) bằng {@code ChatClient.Builder} auto-config của
 * Spring AI. Provider mặc định ({@code matchIfMissing = true}) — {@code llm.provider=groq} chuyển
 * sang {@link GroqProvider} (CLAUDE.md mục 10.7 "Vướng mắc còn tồn đọng").
 */
@Component
@ConditionalOnProperty(prefix = "llm", name = "provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiProvider implements LlmProvider {

    private final ChatClient chatClient;

    /**
     * @param builder {@code ChatClient.Builder} do Spring AI auto-config cung cấp (đã trỏ tới
     *                Gemini qua endpoint tương thích OpenAI, cấu hình ở application.yml)
     */
    public GeminiProvider(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /** {@inheritDoc} */
    @Override
    public String complete(String prompt) {
        return chatClient.prompt(prompt).call().content();
    }
}
