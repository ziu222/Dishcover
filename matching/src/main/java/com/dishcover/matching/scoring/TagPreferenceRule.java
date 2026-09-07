package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;

import java.util.List;
import java.util.Locale;

/**
 * Cộng điểm ưu tiên công thức có tag khớp "định hướng ăn uống" người dùng chọn (VD "high protein"
 * cho gymer, "vegetarian" cho người ăn chay) — chỉ là tín hiệu xếp hạng, KHÔNG phải lọc cứng như
 * {@link AllergyFilterRule}. Không đặt preferredTags, hoặc công thức chưa có tag -> no-op, không
 * ảnh hưởng điểm số cũ (docs/specs/diet-direction-recommendation.md mục 8).
 */
public class TagPreferenceRule implements ScoringRule {

    private static final double BONUS_PER_TAG = 0.5; // cùng độ lớn ExpiryBonusRule

    /**
     * Cộng {@code currentScore} với {@value #BONUS_PER_TAG} điểm cho mỗi tag của công thức khớp
     * (không phân biệt hoa/thường) với tập {@code preferredTags} của người dùng.
     *
     * @param recipe công thức đang được chấm điểm
     * @param ctx dữ liệu người dùng, gồm tập tag ưa thích (có thể rỗng)
     * @param currentScore điểm số tích lũy từ (các) rule trước trong chuỗi
     * @return {@code currentScore} cộng thêm bonus theo số tag khớp, hoặc nguyên trạng nếu thiếu
     *         dữ liệu để so sánh
     */
    @Override
    public double apply(RecipeDetailDto recipe, MatchingContext ctx, double currentScore) {
        List<String> tags = recipe.tags();
        if (ctx.preferredTags().isEmpty() || tags == null) {
            return currentScore;
        }
        long matched = tags.stream()
                .map(t -> t.toLowerCase(Locale.ROOT))
                .filter(ctx.preferredTags()::contains)
                .count();
        return currentScore + BONUS_PER_TAG * matched;
    }
}
