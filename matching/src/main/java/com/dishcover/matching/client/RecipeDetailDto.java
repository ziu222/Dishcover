package com.dishcover.matching.client;

import java.util.List;

/** Map response của GET /recipes/{id} (recipe/dto/RecipeDtos.RecipeDetailResponse) — chỉ lấy field cần dùng để chấm điểm. */
public record RecipeDetailDto(
        String id,
        String name,
        String slug,
        String imageUrl,
        List<RecipeIngredientDto> ingredients
) {
}
