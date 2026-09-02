package com.dishcover.user.dto;

import com.dishcover.user.entity.CalorieGoal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** DTO cho mục tiêu calo/macro/ngày — Matching Service đọc để chấm điểm công thức gần mục tiêu. */
public final class CalorieGoalDtos {

    private CalorieGoalDtos() {
    }

    /**
     * Yêu cầu đặt/sửa mục tiêu — cả 4 field bắt buộc cùng lúc (mục tiêu là 1 bộ số nhất quán,
     * không cho sửa từng phần như dietary-preferences).
     */
    public record CalorieGoalRequest(
            @NotNull @Positive Integer calorieTarget,
            @NotNull @Positive Integer proteinTarget,
            @NotNull @Positive Integer carbTarget,
            @NotNull @Positive Integer fatTarget
    ) {
    }

    /** Mục tiêu calo/macro trả ra API. */
    public record CalorieGoalResponse(
            Integer calorieTarget, Integer proteinTarget, Integer carbTarget, Integer fatTarget
    ) {
        /**
         * Chuyển entity {@link CalorieGoal} sang DTO trả ra API.
         *
         * @param g entity nguồn
         * @return DTO tương ứng
         */
        public static CalorieGoalResponse from(CalorieGoal g) {
            return new CalorieGoalResponse(
                    g.getCalorieTarget(), g.getProteinTarget(), g.getCarbTarget(), g.getFatTarget());
        }
    }
}
