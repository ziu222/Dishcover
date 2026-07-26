package com.dishcover.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình tài liệu OpenAPI (Swagger UI) cho User Service.
 * Khai báo security scheme "bearer-jwt" để nút "Authorize" trên Swagger UI dán được token,
 * sau đó mọi request thử nghiệm tự gắn header Authorization.
 */
@Configuration
public class OpenApiConfiguration {

    private static final String SECURITY_SCHEME = "bearer-jwt";

    @Bean
    OpenAPI userServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Leftover Recipe Matcher — User Service API")
                        .version("0.1.0")
                        .description("""
                                Dịch vụ Người dùng: đăng ký, đăng nhập (JWT), hồ sơ và tùy chọn ăn uống.
                                Thuộc hệ thống gợi ý công thức nấu ăn theo nguyên liệu còn dư — đồ án tốt nghiệp.
                                Endpoint /auth/** công khai; các endpoint khác yêu cầu JWT hợp lệ (bấm Authorize).""")
                        .contact(new Contact()
                                .name("Bùi Trọng Nghĩa — 2351010136, Khoa học máy tính")
                                .email("bietk2812@gmail.com"))
                        .license(new License().name("Academic use")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Dán token lấy từ /auth/login (không cần thêm chữ 'Bearer').")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
