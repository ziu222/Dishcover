package com.dishcover.common.nutrition;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;

import java.util.List;
import java.util.Optional;

/**
 * Tính calo/macro mỗi khẩu phần từ danh sách nguyên liệu 1 công thức — dùng chung cho Recipe
 * Service (tính lúc ghi, lưu vào document — CLAUDE.md phần "hạ tầng dùng chung"), và tái dùng ở
 * Matching Service (so số lượng đủ/thiếu) qua {@link UnitConverter} riêng.
 *
 * <p>Công thức chuẩn: {@code calo = (gram/100) * calo_per_100g}, cộng dồn mọi nguyên liệu rồi chia
 * số khẩu phần. Không tính được 1 nguyên liệu (unit lạ, nguyên liệu ngoài catalog) → bỏ qua đóng góp
 * của nguyên liệu đó, đánh dấu {@link RecipeNutrition#incomplete()}.
 */
public final class RecipeNutritionCalculator {

    private final IngredientCatalog catalog;

    public RecipeNutritionCalculator(IngredientCatalog catalog) {
        this.catalog = catalog;
    }

    public RecipeNutrition calculate(List<NutritionIngredientLine> ingredients, Integer servings) {
        int servingCount = (servings == null || servings <= 0) ? 1 : servings;

        double calories = 0;
        double protein = 0;
        double carb = 0;
        double fat = 0;
        boolean incomplete = false;

        for (NutritionIngredientLine line : ingredients) {
            Optional<IngredientEntry> entryOpt = catalog.lookup(line.normalizedName());
            if (entryOpt.isEmpty() || entryOpt.get().caloriesPer100g() == null) {
                incomplete = true;
                continue;
            }
            IngredientEntry entry = entryOpt.get();
            Optional<Double> grams = UnitConverter.toGrams(line.amount(), line.unit(), entry);
            if (grams.isEmpty()) {
                incomplete = true;
                continue;
            }
            double factor = grams.get() / 100.0;
            calories += factor * entry.caloriesPer100g();
            protein += factor * nullToZero(entry.proteinPer100g());
            carb += factor * nullToZero(entry.carbPer100g());
            fat += factor * nullToZero(entry.fatPer100g());
        }

        return new RecipeNutrition(
                round1(calories / servingCount),
                round1(protein / servingCount),
                round1(carb / servingCount),
                round1(fat / servingCount),
                incomplete
        );
    }

    private static double nullToZero(Double v) {
        return v == null ? 0 : v;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
