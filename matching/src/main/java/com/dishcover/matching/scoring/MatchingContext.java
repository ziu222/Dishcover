package com.dishcover.matching.scoring;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/** Dữ liệu 1 lần fetch từ Inventory/User, dùng chung cho mọi công thức trong 1 request chấm điểm. */
public record MatchingContext(
        Set<String> userNormalizedNames,
        Map<String, LocalDate> expiryByNormalizedName,
        Set<String> userAllergenGroups
) {
}
