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

    /** Overload tiện dùng cho các test/luồng không quan tâm role — mặc định {@code "USER"}. */
    public String issue(Long userId, String email, String plan) {
        return issue(userId, email, plan, "USER");
    }

    public String issue(Long userId, String email, String plan, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("plan", plan)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(expirationMinutes))))
                .signWith(key)
                .compact();
    }

    /** Verify chữ ký + hạn dùng; token không hợp lệ ném JwtException. */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                // Dung sai lệch giờ 60s giữa các service: tránh reject token vừa phát khi đồng hồ
                // service verify lệch vài giây so với service phát (exp/iat/nbf).
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        // role có thể vắng mặt trên token cũ phát trước khi thêm claim này — mặc định "USER" thay vì
        // null, để mọi nơi đọc AuthenticatedUser.role() không phải tự phòng thủ null.
        String role = claims.get("role", String.class);
        return new AuthenticatedUser(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("plan", String.class),
                role != null ? role : "USER");
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }
}
