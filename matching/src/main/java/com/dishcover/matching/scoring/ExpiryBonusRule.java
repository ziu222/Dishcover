package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeIngredientDto;

import java.time.LocalDate;

/**
 * Cộng điểm ưu tiên nguyên liệu sắp hết hạn (≤ 3 ngày) — hiện thực hóa mục tiêu chống lãng phí
 * thực phẩm của đồ án. bao-cao-thuat-toan-matching.md mục 5, specs/matching-service.md mục 3.2 (3).
 */
public class ExpiryBonusRule implements ScoringRule {

    private static final double BONUS_PER_ITEM = 0.5;
    private static final int EXPIRY_THRESHOLD_DAYS = 3;

    @Override
    public double apply(RecipeDetailDto recipe, MatchingContext ctx, double currentScore) {
        LocalDate threshold = LocalDate.now().plusDays(EXPIRY_THRESHOLD_DAYS);

        long expiringMatched = recipe.ingredients().stream()
                .map(RecipeIngredientDto::normalizedName)
                .filter(name -> ctx.userNormalizedNames().contains(name))
                .filter(name -> {
                    LocalDate expiry = ctx.expiryByNormalizedName().get(name);
                    return expiry != null && !expiry.isAfter(threshold);
                })
                .count();

        return currentScore + BONUS_PER_ITEM * expiringMatched;
    }
}
