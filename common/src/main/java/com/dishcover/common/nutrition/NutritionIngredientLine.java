package com.dishcover.common.nutrition;

/**
 * 1 dòng nguyên liệu đầu vào cho {@link RecipeNutritionCalculator} — tách khỏi entity Mongo của
 * Recipe Service để module {@code common} không phụ thuộc ngược vào service cụ thể nào.
 * {@code normalizedName} phải đã qua {@code IngredientCatalog.resolve()} trước khi truyền vào đây
 * (đúng luồng hiện có ở Recipe Service lúc ghi — CLAUDE.md mục 10.5).
 */
public record NutritionIngredientLine(String normalizedName, Double amount, String unit) {
}
