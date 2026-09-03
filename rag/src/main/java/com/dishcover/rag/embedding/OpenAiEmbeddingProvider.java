package com.dishcover.rag.embedding;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * EmbeddingProvider dùng OpenAI thật ({@code text-embedding-3-small}) — nhà cung cấp embedding duy
 * nhất giai đoạn B (Gemini `text-embedding-004` có trong kế hoạch gốc CLAUDE.md mục 6 nhưng
 * GEMINI_API_KEY đang hết credit prepay thật, không live-verify được — xem CLAUDE.md mục 10.7).
 *
 * <p>Tự dựng {@link OpenAiApi}/{@link OpenAiEmbeddingModel} riêng, KHÔNG dùng auto-config —
 * {@code openai.api-key}/{@code openai.base-url} đã dùng chung với {@link com.dishcover.rag.llm.OpenAiProvider}
 * (chat) nên tái dùng nguyên 2 property đó, tách bean riêng vì auto-config Gemini (chat) đã chiếm
 * bean {@code EmbeddingModel} mặc định ở base-url khác.</p>
 *
 * <p>{@code dimensions(768)}: {@code text-embedding-3-small} mặc định trả 1536 chiều, KHÔNG khớp
 * cột {@code vector(768)} đã có sẵn trong lược đồ {@code matching_service.recipe_embeddings}
 * (CLAUDE.md mục 3.1) — dòng embedding-3 hỗ trợ tham số {@code dimensions} rút gọn (Matryoshka),
 * dùng để khớp đúng cột có sẵn thay vì đổi lược đồ (đã ghi chú sẵn từ lúc làm
 * {@link com.dishcover.rag.llm.OpenAiProvider}).</p>
 */
@Component
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private static final int DIMENSIONS = 768;

    private final OpenAiEmbeddingModel embeddingModel;

    public OpenAiEmbeddingProvider(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${openai.embedding-model:text-embedding-3-small}") String model) {
        OpenAiApi openAiApi = OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build();
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(model)
                .dimensions(DIMENSIONS)
                .build();
        this.embeddingModel = new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }

    /** {@inheritDoc} */
    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }
}
