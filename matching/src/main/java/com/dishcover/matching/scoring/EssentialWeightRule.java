package com.dishcover.matching.scoring;

import com.dishcover.common.ingredient.IngredientWeights;
import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeIngredientDto;

import java.util.List;
import java.util.Set;

/**
 * Nhân điểm nền với hệ số coverage có trọng số (essential=1.0, phụ=0.3 — IngredientWeights,
 * cùng nguồn với Recipe Service, tránh 2 nơi tự khai báo lệch nhau).
 * bao-cao-thuat-toan-matching.md mục 4, specs/matching-service.md mục 3.2 (2).
 */
public class EssentialWeightRule implements ScoringRule {

    /**
     * Nhân {@code currentScore} với hệ số coverage có trọng số: tách nguyên liệu công thức thành 2
     * nhóm essential (trọng số {@link IngredientWeights#ESSENTIAL}) và optional (trọng số
     * {@link IngredientWeights#OPTIONAL}), tính coverage riêng từng nhóm rồi gộp theo bình quân gia
     * quyền của 2 trọng số đó.
     *
     * @param recipe công thức đang được chấm điểm
     * @param ctx dữ liệu nguyên liệu của người dùng
     * @param currentScore điểm số tích lũy từ (các) rule trước trong chuỗi (điểm Jaccard nền)
     * @return {@code currentScore} nhân với hệ số coverage có trọng số essential/optional
     */
    @Override
    public double apply(RecipeDetailDto recipe, MatchingContext ctx, double currentScore) {
        List<RecipeIngredientDto> essentialInR = recipe.ingredients().stream()
                .filter(RecipeIngredientDto::essential)
                .toList();
        List<RecipeIngredientDto> optionalInR = recipe.ingredients().stream()
                .filter(i -> !i.essential())
                .toList();

        double essentialCoverage = coverage(essentialInR, ctx.userNormalizedNames());
        double optionalCoverage = coverage(optionalInR, ctx.userNormalizedNames());

        double weightedCoverage = (essentialCoverage * IngredientWeights.ESSENTIAL + optionalCoverage * IngredientWeights.OPTIONAL)
                / (IngredientWeights.ESSENTIAL + IngredientWeights.OPTIONAL);

        return currentScore * weightedCoverage;
    }

    /** Coverage = tỉ lệ nguyên liệu trong danh sách mà người dùng đang có; quy ước = 1.0 nếu danh sách rỗng. */
    private double coverage(List<RecipeIngredientDto> list, Set<String> userNormalizedNames) {
        if (list.isEmpty()) {
            return 1.0;
        }
        long matched = list.stream().filter(i -> userNormalizedNames.contains(i.normalizedName())).count();
        return (double) matched / list.size();
    }
}
