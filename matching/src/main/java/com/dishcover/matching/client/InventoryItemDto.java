package com.dishcover.matching.client;

import java.time.LocalDate;

/**
 * Map response của GET /inventory/items (inventory/dto/InventoryDtos.InventoryItemResponse) — chỉ
 * lấy field cần dùng để chấm điểm + so số lượng đủ/thiếu (availability endpoint).
 *
 * @param id id dòng tồn kho (1 lô) bên Inventory Service — dùng khi cần trỏ lại đúng lô (cook-deduct)
 * @param normalizedName tên nguyên liệu đã chuẩn hóa, dùng làm khóa so khớp với công thức
 * @param quantity số lượng còn lại của lô này, null nếu Inventory Service cũ chưa có field này
 * @param unit đơn vị của {@link #quantity}
 * @param expiryDate hạn dùng dự kiến, null nếu không rõ — dùng cho ExpiryBonusRule
 * @param status trạng thái nguyên liệu (FRESH/EXPIRING_SOON/EXPIRED/USED)
 */
public record InventoryItemDto(
        Long id,
        String normalizedName,
        Double quantity,
        String unit,
        LocalDate expiryDate,
        String status
) {
}
