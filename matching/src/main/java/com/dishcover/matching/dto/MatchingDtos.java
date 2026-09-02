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

    /**
     * So sánh 1 nguyên liệu của công thức với số lượng đang có trong tủ lạnh người dùng.
     *
     * @param name tên hiển thị nguyên liệu
     * @param normalizedName tên đã chuẩn hóa
     * @param neededAmount số lượng cần theo công thức (đơn vị {@link #neededUnit})
     * @param neededUnit đơn vị của {@link #neededAmount}
     * @param availableGrams tổng số gram đang có trong tủ lạnh (0 nếu không có), quy đổi từ mọi lô
     *                       cùng nguyên liệu
     * @param status SUFFICIENT (đủ dùng) | PARTIAL (có nhưng thiếu) | MISSING (không có) | UNKNOWN
     *               (không quy đổi được đơn vị công thức sang gram để so sánh số lượng)
     * @param shortfallAmount số lượng còn thiếu, quy đổi lại theo {@link #neededUnit} — null nếu
     *                        SUFFICIENT hoặc UNKNOWN
     */
    public record IngredientAvailabilityResponse(
            String name,
            String normalizedName,
            Double neededAmount,
            String neededUnit,
            double availableGrams,
            String status,
            Double shortfallAmount
    ) {
    }

    /**
     * Kết quả so đủ/thiếu nguyên liệu của 1 công thức với tủ lạnh người dùng đang đăng nhập —
     * GET /matching/recipes/{id}/availability.
     */
    public record RecipeAvailabilityResponse(
            String recipeId,
            String name,
            List<IngredientAvailabilityResponse> ingredients
    ) {
    }
}
