package com.dishcover.image;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động Image Recognition Service (AI #2, CLAUDE.md mục 7) — stateless, không có DB:
 * nhận ảnh, gọi Vision API, trả đề xuất nguyên liệu để người dùng xác nhận (human-in-the-loop).
 */
@SpringBootApplication
public class ImageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImageServiceApplication.class, args);
    }
}
