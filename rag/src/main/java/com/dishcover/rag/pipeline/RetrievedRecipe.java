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
 * @param dietaryConflicts    tên nguyên liệu (nếu có) vi phạm đặc điểm ăn uống đã khai báo của
 *                            user — tính SẴN bằng code (không để LLM tự suy đoán, xem
 *                            {@link HybridRetriever}), rỗng nếu không xung đột. Công thức khớp
 *                            qua kênh TÊN MÓN (hỏi thẳng) vẫn được giữ lại dù có xung đột — chỉ
 *                            gắn cờ để {@code PromptBuilder} nhắc LLM cảnh báo, không tự loại bỏ.
 */
public record RetrievedRecipe(
        String recipeId,
        String name,
        String slug,
        List<String> matchedIngredients,
        List<String> missingIngredients,
        String imageUrl,
        List<String> dietaryConflicts
) {
}
