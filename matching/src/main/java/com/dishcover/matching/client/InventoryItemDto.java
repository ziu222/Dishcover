package com.dishcover.matching.client;

import java.time.LocalDate;

/**
 * Map response của GET /inventory/items (inventory/dto/InventoryDtos.InventoryItemResponse) — chỉ
 * lấy field cần dùng để chấm điểm.
 *
 * @param normalizedName tên nguyên liệu đã chuẩn hóa, dùng làm khóa so khớp với công thức
 * @param expiryDate hạn dùng dự kiến, null nếu không rõ — dùng cho ExpiryBonusRule
 * @param status trạng thái nguyên liệu (FRESH/EXPIRING_SOON/EXPIRED/USED)
 */
public record InventoryItemDto(
        String normalizedName,
        LocalDate expiryDate,
        String status
) {
}
