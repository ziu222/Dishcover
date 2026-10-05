package com.dishcover.gateway.config;

import com.dishcover.common.security.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Đăng ký {@link JwtService} cho Gateway. Gateway KHÔNG phát token và không chặn request thiếu
 * token (mỗi service tự verify) — chỉ cần đọc claim {@code role} để {@code MaintenanceFilter} cho
 * ADMIN đi qua khi đang bảo trì.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    JwtService jwtService(JwtProperties props) {
        return new JwtService(props.secret(), props.expirationMinutes());
    }
}
