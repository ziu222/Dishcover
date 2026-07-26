package com.dishcover.matching.client;

import java.time.LocalDate;

/** Map response của GET /inventory/items (inventory/dto/InventoryDtos.InventoryItemResponse) — chỉ lấy field cần dùng để chấm điểm. */
public record InventoryItemDto(
        String normalizedName,
        LocalDate expiryDate,
        String status
) {
}
