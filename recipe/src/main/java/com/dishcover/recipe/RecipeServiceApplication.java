package com.dishcover.recipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động Recipe Service — microservice CRUD công thức nấu ăn, lưu dữ liệu trên MongoDB
 * riêng (database {@code recipe_matcher_db}), độc lập với các service dùng PostgreSQL trong hệ
 * thống (CLAUDE.md mục 3).
 */
@SpringBootApplication
public class RecipeServiceApplication {

    /**
     * Khởi chạy Spring Boot application context của Recipe Service.
     *
     * @param args tham số dòng lệnh truyền vào ứng dụng
     */
    public static void main(String[] args) {
        SpringApplication.run(RecipeServiceApplication.class, args);
    }
}
