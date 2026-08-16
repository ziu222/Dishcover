package com.dishcover.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình JWT nạp từ application.yml (jwt.secret, jwt.expiration-minutes). */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes
) {
}
