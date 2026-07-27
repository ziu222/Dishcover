package com.dishcover.rag.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tài liệu OpenAPI cho RAG Service — cùng pattern User/Inventory/Recipe/Matching Service. */
@Configuration
public class OpenApiConfiguration {

    private static final String SECURITY_SCHEME = "bearer-jwt";

    @Bean
    OpenAPI ragServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Leftover Recipe Matcher — RAG Chatbot Service API")
                        .version("0.1.0")
                        .description("""
                                Chatbot trả lời dựa trên công thức thật trong hệ thống (giai đoạn A — lọc cứng qua Matching Service, chưa có vector search).
                                Tính năng PRO — cần JWT hợp lệ với claim plan=PRO (bấm Authorize).
                                Đồ án tốt nghiệp — Bùi Trọng Nghĩa, 2351010136, KHMT.""")
                        .contact(new Contact()
                                .name("Bùi Trọng Nghĩa — 2351010136, Khoa học máy tính")
                                .email("bietk2812@gmail.com"))
                        .license(new License().name("Academic use")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Dán token lấy từ User Service /auth/login. Cần claim plan=PRO.")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
