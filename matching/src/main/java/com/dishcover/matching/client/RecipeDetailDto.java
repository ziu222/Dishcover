package com.dishcover.matching.client;

import java.util.List;

/**
 * Map response của GET /recipes/{id} (recipe/dto/RecipeDtos.RecipeDetailResponse) — chỉ lấy field
 * cần dùng để chấm điểm.
 *
 * @param id id công thức bên MongoDB (Recipe Service)
 * @param name tên hiển thị của công thức
 * @param slug slug dùng cho URL thân thiện
 * @param imageUrl URL ảnh minh họa công thức, có thể null
 * @param ingredients danh sách nguyên liệu của công thức, dùng làm tập R trong Jaccard
 */
public record RecipeDetailDto(
        String id,
        String name,
        String slug,
        String imageUrl,
        List<RecipeIngredientDto> ingredients
) {
}
