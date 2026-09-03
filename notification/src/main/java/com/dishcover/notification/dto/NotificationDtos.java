package com.dishcover.notification.dto;

import java.time.Instant;
import java.util.List;

public final class NotificationDtos {

    private NotificationDtos() {
    }

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

    public record NotificationListResponse(
            List<NotificationResponse> items,
            long unreadCount
    ) {
    }
}
