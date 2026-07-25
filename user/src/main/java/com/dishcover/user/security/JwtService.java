package com.dishcover.user.security;

import com.dishcover.user.config.JwtProperties;
import com.dishcover.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Ký và verify JWT bằng HMAC-SHA256. Token mang claim "plan" để Gateway gating FREE/PRO (mục 8).
 * Gateway dùng CÙNG secret này để verify — chia sẻ qua env/Config Server.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(JwtProperties props) {
        byte[] secretBytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET phải >= 32 ký tự cho HS256 (hiện: " + secretBytes.length + ")");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMinutes = props.expirationMinutes();
    }

    public String issue(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("plan", user.getPlan())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(expirationMinutes))))
                .signWith(key)
                .compact();
    }

    /** Verify chữ ký + hạn dùng, trả principal. Ném JwtException nếu token sai/hết hạn. */
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
