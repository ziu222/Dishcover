package com.dishcover.rag.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Duy nhất class trong module được import {@code org.springframework.ai.chat.client.ChatClient}
 * (CLAUDE.md mục 9 "D") — gọi Gemini qua endpoint tương thích OpenAI (spring-ai-starter-model-openai,
 * cấu hình ở application.yml, specs/rag-service.md mục 3.5).
 */
@Component
public class GeminiProvider implements LlmProvider {

    private final ChatClient chatClient;

    public GeminiProvider(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String complete(String prompt) {
        return chatClient.prompt(prompt).call().content();
    }
}
