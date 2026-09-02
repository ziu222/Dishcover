package com.dishcover.matching.client;

/**
 * Map response của GET /users/me/calorie-goal (user/dto/CalorieGoalDtos.CalorieGoalResponse) — chỉ
 * lấy field cần dùng cho CalorieProximityRule.
 *
 * @param calorieTarget calo mục tiêu/ngày, null nếu user chưa đặt mục tiêu (body rỗng từ User Service)
 */
public record CalorieGoalDto(Integer calorieTarget) {
}
