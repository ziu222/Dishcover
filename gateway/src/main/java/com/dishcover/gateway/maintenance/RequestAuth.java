package com.dishcover.gateway.maintenance;

import com.dishcover.common.security.AuthenticatedUser;
import com.dishcover.common.security.JwtAuthFilter;
import com.dishcover.common.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * Đọc + verify JWT từ request WebFlux, cùng thứ tự ưu tiên với {@link JwtAuthFilter} phía các
 * service khác (header Bearer trước, cookie {@code auth_token} sau) — nhưng viết riêng ở đây vì
 * JwtAuthFilter dựa trên Servlet API, không dùng được trong Gateway (WebFlux).
 */
@Component
public class RequestAuth {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public RequestAuth(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /** Trả về user đã xác thực, hoặc {@code null} nếu không có token/token không hợp lệ. */
    public AuthenticatedUser resolve(ServerHttpRequest request) {
        String token = extractToken(request);
        if (token == null) return null;
        try {
            return jwtService.parse(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isAdmin(ServerHttpRequest request) {
        AuthenticatedUser user = resolve(request);
        return user != null && "ADMIN".equals(user.role());
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        var cookie = request.getCookies().getFirst(JwtAuthFilter.COOKIE_NAME);
        return cookie != null ? cookie.getValue() : null;
    }
}
