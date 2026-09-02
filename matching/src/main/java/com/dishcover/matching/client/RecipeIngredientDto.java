package com.dishcover.matching.client;

/**
 * Map 1 phần tử ingredients[] của RecipeDetailResponse (recipe/dto/RecipeDtos.RecipeIngredientResponse).
 *
 * @param name tên hiển thị (có dấu) — dùng để trả về client trong matchedIngredients/missingIngredients,
 *             KHÔNG dùng để so khớp (xem {@link #normalizedName})
 * @param normalizedName tên nguyên liệu đã chuẩn hóa, dùng làm khóa so khớp với tủ lạnh người dùng
 * @param amount số lượng cần, dùng cho availability endpoint (so số lượng đủ/thiếu) — KHÔNG dùng
 *               cho thuật toán chấm điểm Jaccard/weight/expiry (chỉ cần có/không có)
 * @param unit đơn vị của {@link #amount}
 * @param essential true nếu là nguyên liệu chính (weight 1.0), false nếu là nguyên liệu phụ (weight 0.3)
 * @param weight trọng số của Recipe Service ghi lại lúc tạo công thức — Matching Service KHÔNG đọc
 *               trực tiếp field này, tự tính lại từ {@link #essential} qua
 *               {@code com.dishcover.common.ingredient.IngredientWeights} (CLAUDE.md mục 3.2)
 */
public record RecipeIngredientDto(
        String name,
        String normalizedName,
        Double amount,
        String unit,
        boolean essential,
        double weight
) {
}
