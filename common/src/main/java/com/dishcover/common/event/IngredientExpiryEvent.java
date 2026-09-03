package com.dishcover.common.event;

import java.time.LocalDate;

/**
 * Sự kiện Kafka: 1 nguyên liệu của 1 user vừa được xác nhận đang ở trạng thái sắp/đã hết hạn.
 * Dùng chung giữa Inventory Service (producer) và Notification Service (consumer) — schema
 * message, KHÔNG phải truy cập DB chéo (specs/notification-service.md mục 3).
 */
public record IngredientExpiryEvent(
        Long userId,
        Long inventoryItemId,
        String ingredientName,
        String normalizedName,
        LocalDate expiryDate,
        String status
) {
    public static final String TOPIC = "ingredient-expiry-events";
}
