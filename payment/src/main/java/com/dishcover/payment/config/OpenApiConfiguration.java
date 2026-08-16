package com.dishcover.payment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tài liệu OpenAPI cho Payment Service — cùng pattern User/Inventory/Recipe Service. */
@Configuration
public class OpenApiConfiguration {

    private static final String SECURITY_SCHEME = "bearer-jwt";

    /**
     * @return cấu hình OpenAPI (thông tin API + security scheme bearer-jwt) hiển thị trên Swagger UI
     */
    @Bean
    OpenAPI paymentServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Leftover Recipe Matcher — Payment Service API")
                        .version("0.1.0")
                        .description("""
                                Thanh toán nâng cấp gói PRO qua VNPay sandbox.
                                Xem bảng giá (/payments/plans) KHÔNG cần đăng nhập; tạo giao dịch và tra cứu
                                trạng thái yêu cầu JWT hợp lệ (lấy từ /auth/login của User Service — bấm Authorize).
                                Endpoint IPN do cổng thanh toán gọi server-to-server, không dùng JWT mà xác thực
                                bằng chữ ký HMAC-SHA512.
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
