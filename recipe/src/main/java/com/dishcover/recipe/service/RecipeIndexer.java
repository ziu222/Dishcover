package com.dishcover.recipe.service;

import com.dishcover.recipe.client.MatchingIndexClient;
import com.dishcover.recipe.client.RagIndexClient;
import com.dishcover.recipe.entity.Recipe;
import com.dishcover.recipe.entity.RecipeIngredient;
import com.dishcover.recipe.entity.RecipeStep;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Index công thức vào {@code recipe_embeddings} (Giai đoạn B — RAG vector search, CLAUDE.md mục 6)
 * sau khi tạo/sửa. Chạy nền ({@code @Async}, xem {@code AsyncConfig}) — response
 * {@code POST}/{@code PATCH /recipes} KHÔNG chờ bước này. Cả 2 lời gọi (RAG embed, Matching lưu)
 * đều fail-open ở tầng client — lớp này không cần try/catch thêm.
 */
@Component
public class RecipeIndexer {

    private final RagIndexClient ragIndexClient;
    private final MatchingIndexClient matchingIndexClient;

    public RecipeIndexer(RagIndexClient ragIndexClient, MatchingIndexClient matchingIndexClient) {
        this.ragIndexClient = ragIndexClient;
        this.matchingIndexClient = matchingIndexClient;
    }

    /**
     * @param bearerToken token JWT của người tạo/sửa công thức, chuyển tiếp cho RAG/Matching
     * @param recipe      công thức VỪA lưu (đã có id thật)
     */
    @Async
    public void indexAsync(String bearerToken, Recipe recipe) {
        indexSync(bearerToken, recipe);
    }

    /**
     * Lõi đồng bộ dùng chung cho {@link #indexAsync} (tạo/sửa 1 công thức, chạy nền) và backfill
     * hàng loạt ({@code RecipeIndexBackfillRunner} — CẦN đồng bộ để runner biết khi nào xong trước
     * lúc thoát tiến trình, gọi {@link #indexAsync} ở đó sẽ mất hết việc đang chạy nền khi JVM tắt).
     *
     * @param bearerToken token JWT chuyển tiếp cho RAG/Matching
     * @param recipe      công thức cần index
     */
    public void indexSync(String bearerToken, Recipe recipe) {
        String content = buildContent(recipe);
        Optional<float[]> embedding = ragIndexClient.embed(bearerToken, content);
        if (embedding.isEmpty()) {
            return; // đã log warn ở fallbackEmbed, không có gì để lưu
        }
        matchingIndexClient.index(bearerToken, recipe.getId(), content, embedding.get(),
                Map.of("name", recipe.getName()));
    }

    /**
     * Văn bản đại diện: tên, nguyên liệu, tag, thời gian, tóm tắt cách nấu (đúng thành phần CLAUDE.md
     * mục 6 "Indexing pipeline" liệt kê).
     */
    private String buildContent(Recipe recipe) {
        String ingredients = recipe.getIngredients().stream()
                .map(RecipeIngredient::getName)
                .collect(Collectors.joining(", "));
        String tags = String.join(", ", recipe.getTags());
        String steps = recipe.getSteps().stream()
                .map(RecipeStep::getContent)
                .collect(Collectors.joining(" "));
        return "%s. Nguyên liệu: %s. Thời gian nấu: %d phút. Loại: %s. Cách làm: %s"
                .formatted(recipe.getName(), ingredients, recipe.getCookTimeMinutes(), tags, steps);
    }
}
