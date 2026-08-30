package com.dishcover.matching.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Gom nhóm DTO request/response của Matching Service (namespace, không khởi tạo được). */
public final class MatchingDtos {

    private MatchingDtos() {
    }

    /**
     * DTO trả về cho client — 1 công thức được gợi ý kèm điểm số và phần nguyên liệu đã/còn thiếu.
     *
     * @param recipeId id công thức bên Recipe Service (MongoDB)
     * @param name tên hiển thị của công thức
     * @param slug slug dùng cho URL thân thiện
     * @param score điểm số cuối cùng sau khi chạy hết chuỗi {@link com.dishcover.matching.scoring.ScoringRule}
     * @param matchedIngredients tên hiển thị (có dấu) nguyên liệu người dùng đang có và công thức cần —
     *                            so khớp nội bộ dùng normalizedName, nhưng trả ra client bằng tên thật
     * @param missingIngredients tên hiển thị (có dấu) nguyên liệu công thức cần nhưng người dùng chưa có
     * @param imageUrl URL ảnh minh họa công thức, có thể null
     */
    public record RecipeMatchResponse(
            String recipeId,
            String name,
            String slug,
            double score,
            List<String> matchedIngredients,
            List<String> missingIngredients,
            String imageUrl
    ) {
    }

    /**
     * Dùng cho POST /internal/match-by-ingredients — RAG gọi với nguyên liệu trích từ câu hỏi chat.
     *
     * @param ingredients tên nguyên liệu tự do trích xuất từ câu hỏi chat, không được rỗng
     * @param topN số lượng kết quả tối đa mong muốn; null dùng mặc định của {@code MatchingService}
     */
    public record MatchByIngredientsRequest(
            @NotEmpty List<String> ingredients,
            Integer topN
    ) {
    }
}
