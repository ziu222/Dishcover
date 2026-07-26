package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeIngredientDto;

import java.util.List;
import java.util.Set;

/**
 * Nhân điểm nền với hệ số coverage có trọng số (essential=1.0, phụ=0.3).
 * bao-cao-thuat-toan-matching.md mục 4, specs/matching-service.md mục 3.2 (2).
 */
public class EssentialWeightRule implements ScoringRule {

    private static final double ESSENTIAL_WEIGHT = 1.0;
    private static final double OPTIONAL_WEIGHT = 0.3;

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

        double weightedCoverage = (essentialCoverage * ESSENTIAL_WEIGHT + optionalCoverage * OPTIONAL_WEIGHT)
                / (ESSENTIAL_WEIGHT + OPTIONAL_WEIGHT);

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
