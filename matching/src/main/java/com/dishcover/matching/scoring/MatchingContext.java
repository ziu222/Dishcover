package com.dishcover.matching.scoring;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Dữ liệu 1 lần fetch từ Inventory/User, dùng chung cho mọi công thức trong 1 request chấm điểm.
 *
 * @param userNormalizedNames tập tên nguyên liệu đã chuẩn hóa mà người dùng đang có (tập U trong Jaccard)
 * @param expiryByNormalizedName hạn dùng theo từng nguyên liệu đang có, dùng cho ExpiryBonusRule
 * @param userAllergenGroups tập nhóm dị ứng của người dùng, dùng cho AllergyFilterRule
 */
public record MatchingContext(
        Set<String> userNormalizedNames,
        Map<String, LocalDate> expiryByNormalizedName,
        Set<String> userAllergenGroups
) {
}
