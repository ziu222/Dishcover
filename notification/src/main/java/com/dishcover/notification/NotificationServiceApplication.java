package com.dishcover.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động Notification Service — cảnh báo hết hạn nguyên liệu qua Kafka, schema
 * {@code notification_service} trên PostgreSQL dùng chung (specs/notification-service.md).
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
