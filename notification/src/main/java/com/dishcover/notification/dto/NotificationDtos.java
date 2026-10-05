package com.dishcover.notification.dto;

import java.time.Instant;
import java.util.List;

/** DTO trả ra API của Notification Service (record, không expose entity JPA — CLAUDE.md mục 9). */
public final class NotificationDtos {

    private NotificationDtos() {
    }

    /**
     * 1 thông báo hiển thị trong dropdown chuông.
     *
     * @param type      INGREDIENT_EXPIRING_SOON | INGREDIENT_EXPIRED
     * @param actionUrl đường dẫn frontend khi bấm vào (VD /goi-y?ingredient=ca chua)
     */
    public record NotificationResponse(
            Long id,
            String type,
            String title,
            String message,
            String actionUrl,
            boolean isRead,
            Instant createdAt
    ) {
    }

    /** @param unreadCount tổng số chưa đọc của user (không phụ thuộc trang đang xem) */
    public record NotificationListResponse(
            List<NotificationResponse> items,
            long unreadCount
    ) {
    }
}
