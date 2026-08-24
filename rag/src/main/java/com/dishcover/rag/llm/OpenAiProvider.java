package com.dishcover.rag.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * LlmProvider dùng OpenAI thật — kích hoạt bằng {@code llm.provider=openai}.
 *
 * <p>Ưu điểm so với hai nhà cung cấp còn lại: MỘT khóa phủ được cả ba nhu cầu AI của hệ thống
 * (chat cho RAG, thị giác cho Image Recognition ở mục 7, và embedding cho RAG giai đoạn B) —
 * đúng tính chất khiến CLAUDE.md mục 6 chọn Gemini ban đầu, mà Groq không có (Groq thiếu cả
 * model thị giác lẫn embedding).</p>
 *
 * <p>Tự dựng {@link OpenAiApi}/{@link OpenAiChatModel} riêng thay vì dùng {@code ChatClient.Builder}
 * auto-config, cùng lý do với {@link GroqProvider}: bean auto-config đã bị cấu hình Gemini chiếm,
 * hai nhà cung cấp không dùng chung một bean được. {@code base-url} để mặc định của thư viện
 * ({@code https://api.openai.com}) — KHÔNG thêm {@code /v1} vì {@link OpenAiApi} tự nối
 * {@code /v1/chat/completions}, đúng cái bẫy đã gặp thật lúc live-verify Groq.</p>
 *
 * <p>Lưu ý cho giai đoạn B: cột {@code embedding vector(768)} trong lược đồ (mục 3.1) không khớp
 * mặc định 1536 chiều của {@code text-embedding-3-small}; dòng embedding-3 nhận tham số
 * {@code dimensions} để rút còn 768, nhớ đặt khi cấu hình vector store thay vì đổi lược đồ.</p>
 */
@Component
@ConditionalOnProperty(prefix = "llm", name = "provider", havingValue = "openai")
public class OpenAiProvider implements LlmProvider {

    private final ChatClient chatClient;

    public OpenAiProvider(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${openai.model:gpt-4o-mini}") String model) {
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
