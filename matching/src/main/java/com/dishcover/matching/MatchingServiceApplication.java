package com.dishcover.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động Spring Boot của Matching Service — gợi ý công thức theo nguyên liệu người dùng
 * đang có, dùng thuật toán Weighted Jaccard + expiry bonus + lọc dị ứng (CLAUDE.md mục 5).
 */
@SpringBootApplication
public class MatchingServiceApplication {
    /**
     * Khởi chạy Matching Service dưới dạng ứng dụng Spring Boot độc lập.
     *
     * @param args tham số dòng lệnh truyền cho Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(MatchingServiceApplication.class, args);
    }
}
