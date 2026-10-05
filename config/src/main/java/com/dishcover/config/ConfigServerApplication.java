package com.dishcover.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server (backend "native", đọc classpath:/configs). Hiện chỉ là khung: chưa
 * service nào import cấu hình từ đây — mỗi service vẫn tự có application.yml + biến môi trường.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
