package com.dishcover.common.nutrition;

/**
 * Kết quả tính calo/macro cho 1 khẩu phần (1 serving) của 1 công thức.
 * {@code incomplete=true} khi có ≥1 nguyên liệu không quy đổi được sang gram (thiếu unit trong
 * {@link UnitConverter}/{@code unitToGram} riêng, hoặc nguyên liệu lạ không có trong catalog) —
 * số liệu vẫn trả về (phần tính được), KHÔNG chặn hiển thị công thức (CLAUDE.md "không màn hình lỗi
 * trắng"), chỉ là ước tính chưa đầy đủ.
 */
public record RecipeNutrition(
        double caloriesPerServing,
        double proteinPerServing,
        double carbPerServing,
        double fatPerServing,
        boolean incomplete
) {
    public static final RecipeNutrition EMPTY = new RecipeNutrition(0, 0, 0, 0, true);
}
