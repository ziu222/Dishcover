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
 * @param nutrition calo/macro mỗi khẩu phần, null nếu Recipe Service cũ chưa có field này
 */
public record RecipeDetailDto(
        String id,
        String name,
        String slug,
        String imageUrl,
        List<RecipeIngredientDto> ingredients,
        NutritionDto nutrition
) {
    /**
     * Map subset của {@code recipe/dto/RecipeDtos.NutritionResponse} — chỉ lấy field calo cần dùng
     * để chấm điểm (CalorieProximityRule), bỏ qua protein/carb/fat/incomplete không dùng ở đây.
     *
     * @param caloriesPerServing calo mỗi khẩu phần, null nếu Recipe Service không tính được
     */
    public record NutritionDto(Double caloriesPerServing) {
    }
}
