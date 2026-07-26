package com.dishcover.inventory.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tài liệu OpenAPI cho Inventory Service — cùng pattern User Service. */
@Configuration
public class OpenApiConfiguration {

    private static final String SECURITY_SCHEME = "bearer-jwt";

    @Bean
    OpenAPI inventoryServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Leftover Recipe Matcher — Inventory Service API")
                        .version("0.1.0")
                        .description("""
                                Dịch vụ Tủ lạnh ảo: quản lý nguyên liệu người dùng đang có, số lượng, hạn dùng.
                                Toàn bộ endpoint yêu cầu JWT hợp lệ (lấy từ /auth/login của User Service — bấm Authorize).
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
                                .description("Dán token lấy từ User Service /auth/login.")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
