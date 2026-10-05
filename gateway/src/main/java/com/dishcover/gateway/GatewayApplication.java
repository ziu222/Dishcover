package com.dishcover.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway (Spring Cloud Gateway, WebFlux) — cổng DUY NHẤT expose port ra ngoài (CLAUDE.md mục
 * 3 "Private Network"). Route khai báo tường minh trong application.yml (không Eureka): mỗi prefix
 * {@code /<tên>-service/**} chuyển tới đúng service, StripPrefix bỏ prefix trước khi chuyển. Ngoài
 * định tuyến chỉ có thêm CORS và chế độ bảo trì (package {@code maintenance}).
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
