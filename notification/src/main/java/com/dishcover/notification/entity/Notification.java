package com.dishcover.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Map bảng notification_service.notifications. {@code uniqueConstraints} khai báo lại đúng
 * {@code ux_notifications_dedup} đã có trong V1__init.sql (Flyway) — cần thiết để test
 * {@code @DataJpaTest} (Flyway tắt, Hibernate ddl-auto=create-drop) dựng đúng schema có ràng buộc
 * dedup, không chỉ để tài liệu hoá; migration thật ở Postgres không đổi.
 */
@Entity
@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "ux_notifications_dedup",
                columnNames = {"user_id", "source_inventory_item_id", "type"}))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "source_inventory_item_id", nullable = false)
    private Long sourceInventoryItemId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(Long userId, String type, String title, String message,
                         String actionUrl, Long sourceInventoryItemId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.actionUrl = actionUrl;
        this.sourceInventoryItemId = sourceInventoryItemId;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public Long getSourceInventoryItemId() {
        return sourceInventoryItemId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
