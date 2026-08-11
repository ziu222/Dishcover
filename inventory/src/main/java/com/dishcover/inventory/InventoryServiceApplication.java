package com.dishcover.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động Inventory Service (Tủ lạnh ảo) — microservice quản lý nguyên liệu
 * người dùng đang có trong tủ lạnh, kết nối schema {@code inventory_service} trên
 * PostgreSQL dùng chung (xem CLAUDE.md mục 3).
 */
@SpringBootApplication
public class InventoryServiceApplication {

    /**
     * Khởi chạy Spring Boot application context của Inventory Service.
     *
     * @param args tham số dòng lệnh truyền cho Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
