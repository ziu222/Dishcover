package com.dishcover.common.ingredient;

import java.util.List;
import java.util.Map;

/**
 * 1 mục trong Ingredient Catalog (CLAUDE.md mục 3.3).
 * normalizedName là khóa so khớp thật giữa Inventory/Recipe/Matching/RAG.
 *
 * <p>Nhóm field dinh dưỡng (caloriesPer100g..fatPer100g/defaultState) dùng bởi
 * {@link com.dishcover.common.nutrition.RecipeNutritionCalculator} — ước lượng tham khảo
 * USDA FoodData Central/bảng thành phần dinh dưỡng VN, không phải dữ liệu y tế chính xác tuyệt đối.
 * defaultState RAW|COOKED quyết định số liệu tính trên trạng thái nào (sống/chín lệch 2-3 lần calo).
 * unitToGram: quy đổi CHỈ cho đơn vị ĐẾM riêng của nguyên liệu này (VD "quả"→50g cho trứng gà) —
 * null/rỗng nếu nguyên liệu không dùng đơn vị đếm. Đơn vị đo lường chung (g/kg/ml/muỗng...) quy đổi
 * ở {@link com.dishcover.common.nutrition.UnitConverter}, không lặp lại ở đây.
 */
public record IngredientEntry(
        String canonicalName,
        String normalizedName,
        List<String> aliases,
        String category,
        Integer shelfLifeDays,
        String allergenGroup,
        Double caloriesPer100g,
        Double proteinPer100g,
        Double carbPer100g,
        Double fatPer100g,
        String defaultState,
        Map<String, Double> unitToGram
) {
    /** Entry không cần dữ liệu dinh dưỡng (test fixture ngoài phạm vi tính calo) — nutrition null. */
    public static IngredientEntry basic(String canonicalName, String normalizedName, List<String> aliases,
                                         String category, Integer shelfLifeDays, String allergenGroup) {
        return new IngredientEntry(canonicalName, normalizedName, aliases, category, shelfLifeDays,
                allergenGroup, null, null, null, null, null, null);
    }
}
