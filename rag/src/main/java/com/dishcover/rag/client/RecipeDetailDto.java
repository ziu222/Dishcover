package com.dishcover.rag.client;

import java.util.List;

/** Mirror phần cần dùng của Recipe Service RecipeDetailResponse — chỉ tên + nguyên liệu, không cần steps/tags. */
public record RecipeDetailDto(String id, String name, String slug, List<RecipeIngredientDto> ingredients) {
}
