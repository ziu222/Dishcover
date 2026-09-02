package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;

/**
 * Cộng điểm ưu tiên công thức có calo/khẩu phần gần mục tiêu/bữa của người dùng (mục tiêu/ngày chia
 * 3, xem {@link MatchingContext#calorieTargetPerMeal()}). Không set mục tiêu, hoặc Recipe Service
 * chưa tính được calo (nutrition null/incomplete) -> no-op, không ảnh hưởng điểm số cũ.
 */
public class CalorieProximityRule implements ScoringRule {

    private static final double MAX_BONUS = 1.0;

    /**
     * Cộng {@code currentScore} với tối đa {@value #MAX_BONUS} điểm, giảm dần tuyến tính theo độ
     * lệch tương đối giữa calo/khẩu phần của công thức và mục tiêu/bữa — lệch bằng hoặc vượt 100%
     * mục tiêu thì không cộng gì (không trừ điểm, chỉ đơn giản là không thưởng).
     *
     * @param recipe công thức đang được chấm điểm
     * @param ctx dữ liệu người dùng, gồm mục tiêu calo/bữa (có thể null)
     * @param currentScore điểm số tích lũy từ (các) rule trước trong chuỗi
     * @return {@code currentScore} cộng thêm bonus theo độ gần mục tiêu calo, hoặc nguyên trạng nếu
     *         thiếu dữ liệu để so sánh
     */
    @Override
    public double apply(RecipeDetailDto recipe, MatchingContext ctx, double currentScore) {
        Integer target = ctx.calorieTargetPerMeal();
        Double calories = recipe.nutrition() == null ? null : recipe.nutrition().caloriesPerServing();
        if (target == null || target <= 0 || calories == null) {
            return currentScore;
        }
        double diff = Math.abs(calories - target);
        double bonus = Math.max(0, 1 - diff / target);
        return currentScore + bonus * MAX_BONUS;
    }
}
