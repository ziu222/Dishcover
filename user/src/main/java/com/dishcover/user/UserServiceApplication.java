package com.dishcover.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi chạy Spring Boot cho User Service — dịch vụ đăng ký/đăng nhập (JWT) và quản lý
 * hồ sơ ăn uống (dietary preferences) trong kiến trúc microservices Leftover Recipe Matcher.
 */
@SpringBootApplication
public class UserServiceApplication {

    /**
     * Khởi động ứng dụng Spring Boot của User Service.
     *
     * @param args tham số dòng lệnh truyền cho Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
