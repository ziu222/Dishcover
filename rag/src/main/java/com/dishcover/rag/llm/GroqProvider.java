package com.dishcover.rag.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * LlmProvider dự phòng dùng Groq (endpoint tương thích OpenAI). {@code groq.base-url} mặc định
 * {@code https://api.groq.com/openai} — KHÔNG thêm {@code /v1}: {@link OpenAiApi} tự nối
 * {@code /v1/chat/completions} vào base-url (giống {@code DEFAULT_BASE_URL} của OpenAI thật là
 * {@code https://api.openai.com}, không phải {@code .../v1} — verify qua log lỗi 404
 * {@code "Unknown request URL: POST /openai/v1/v1/chat/completions"} lúc live-verify thật, xem
 * CLAUDE.md mục 10.7 "Vướng mắc còn tồn đọng"). Kích hoạt bằng {@code llm.provider=groq}, dùng khi
 * Gemini bị chặn quota. Tự dựng {@link OpenAiApi}/{@link OpenAiChatModel} riêng thay vì dùng
 * {@code ChatClient.Builder} auto-config (bean đó đã bị chiếm bởi cấu hình Gemini trong
 * application.yml — 2 nhà cung cấp không thể dùng chung 1 bean auto-config).
 */
@Component
@ConditionalOnProperty(prefix = "llm", name = "provider", havingValue = "groq")
public class GroqProvider implements LlmProvider {

    private final ChatClient chatClient;

    public GroqProvider(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.base-url:https://api.groq.com/openai}") String baseUrl,
            @Value("${groq.model:llama-3.3-70b-versatile}") String model) {
        OpenAiApi openAiApi = OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /** {@inheritDoc} */
    @Override
    public String complete(String prompt) {
        return chatClient.prompt(prompt).call().content();
    }
}
