package com.dishcover.matching.client;

/**
 * Map response của GET /users/me/dietary-preferences (user/dto/DietaryDtos.DietaryPreferenceResponse).
 *
 * @param id id bản ghi hồ sơ ăn uống
 * @param type loại hồ sơ (ALLERGY | DIET) — Matching Service chỉ dùng type=ALLERGY
 * @param value giá trị tự do người dùng nhập (VD "hải sản"), được chuẩn hóa thành allergenGroup
 */
public record DietaryPreferenceDto(Long id, String type, String value) {
}
