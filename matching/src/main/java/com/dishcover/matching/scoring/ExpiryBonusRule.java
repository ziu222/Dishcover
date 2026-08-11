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

    /**
     * Cộng {@code currentScore} với 0.5 điểm cho mỗi nguyên liệu vừa khớp với người dùng (matched)
     * vừa có hạn dùng còn lại ≤ 3 ngày kể từ hôm nay.
     *
     * @param recipe công thức đang được chấm điểm
     * @param ctx dữ liệu nguyên liệu (kèm hạn dùng) của người dùng
     * @param currentScore điểm số tích lũy từ (các) rule trước trong chuỗi
     * @return {@code currentScore} cộng thêm 0.5 nhân số nguyên liệu matched sắp hết hạn
     */
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
