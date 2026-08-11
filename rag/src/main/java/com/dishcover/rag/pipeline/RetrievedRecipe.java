package com.dishcover.rag.pipeline;

import java.util.List;

/**
 * Công thức ứng viên đã qua {@link HybridRetriever}, dùng để dựng prompt (PromptBuilder) và trả
 * về {@code sourceRecipeIds} cho client — tách khỏi {@code RecipeMatchDto} (DTO của Matching
 * Service) để pipeline nội bộ của RAG không phụ thuộc trực tiếp shape response bên ngoài.
 *
 * @param recipeId           id công thức bên Recipe Service (MongoDB {@code _id})
 * @param name                tên công thức
 * @param slug                slug công thức
 * @param matchedIngredients  nguyên liệu công thức khớp với nguyên liệu người dùng nhắc tới
 * @param missingIngredients  nguyên liệu công thức còn thiếu so với người dùng
 * @param imageUrl            ảnh minh họa công thức (Cloudinary), có thể null
 */
public record RetrievedRecipe(
        String recipeId,
        String name,
        String slug,
        List<String> matchedIngredients,
        List<String> missingIngredients,
        String imageUrl
) {
}
