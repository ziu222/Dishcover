package com.dishcover.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Ký và verify JWT HS256 dùng chung giữa các service.
 * Service sở hữu đăng nhập truyền các claim đã được kiểm soát thay vì entity cụ thể,
 * để common không phụ thuộc vào module nghiệp vụ nào.
 */
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(String secret, long expirationMinutes) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET phải >= 32 ký tự cho HS256 (hiện: " + secretBytes.length + ")");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMinutes = expirationMinutes;
    }

    public String issue(Long userId, String email, String plan) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("plan", plan)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(expirationMinutes))))
                .signWith(key)
                .compact();
    }

    /** Verify chữ ký + hạn dùng; token không hợp lệ ném JwtException. */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthenticatedUser(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("plan", String.class));
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }
}
