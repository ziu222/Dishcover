package com.dishcover.matching.scoring;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;
import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeIngredientDto;

/**
 * Loại cứng công thức chứa nguyên liệu thuộc nhóm dị ứng người dùng khai báo — ràng buộc an toàn,
 * không phải điểm số. bao-cao-thuat-toan-matching.md mục 6, specs/matching-service.md mục 3.2 (4).
 */
public class AllergyFilterRule implements ScoringRule {

    private final IngredientCatalog catalog;

    /**
     * @param catalog từ điển nguyên liệu chuẩn hóa, dùng để tra {@code allergenGroup} của từng
     *                nguyên liệu trong công thức
     */
    public AllergyFilterRule(IngredientCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Loại cứng công thức nếu có bất kỳ nguyên liệu nào thuộc nhóm dị ứng người dùng khai báo —
     * chạy cuối chuỗi rule (sau khi đã tính điểm) và ghi đè toàn bộ điểm số bằng
     * {@link Double#NEGATIVE_INFINITY} thay vì trừ điểm, đảm bảo công thức bị loại tuyệt đối khỏi
     * kết quả sort/limit ở {@link com.dishcover.matching.service.MatchingService}.
     *
     * @param recipe công thức đang được chấm điểm
     * @param ctx dữ liệu dị ứng của người dùng
     * @param currentScore điểm số tích lũy từ (các) rule trước trong chuỗi
     * @return {@code currentScore} nếu không vi phạm dị ứng; {@link Double#NEGATIVE_INFINITY} nếu vi phạm
     */
    @Override
    public double apply(RecipeDetailDto recipe, MatchingContext ctx, double currentScore) {
        boolean violatesAllergy = recipe.ingredients().stream().anyMatch(i -> violates(i, ctx));
        return violatesAllergy ? Double.NEGATIVE_INFINITY : currentScore;
    }

    private boolean violates(RecipeIngredientDto ingredient, MatchingContext ctx) {
        return catalog.lookup(ingredient.normalizedName())
                .map(IngredientEntry::allergenGroup)
                .filter(group -> group != null && ctx.userAllergenGroups().contains(group))
                .isPresent();
    }
}
