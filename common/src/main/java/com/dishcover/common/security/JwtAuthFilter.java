package com.dishcover.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Đọc token, verify và đặt principal dùng chung vào SecurityContext.
 * Ưu tiên header {@code Authorization: Bearer} (tiện test qua Swagger/curl không cần cookie jar),
 * rơi về cookie {@value #COOKIE_NAME} nếu không có header — đây là đường trình duyệt dùng
 * sau khi chuyển sang httpOnly cookie (JS không đọc/đặt được Authorization header cho cookie này).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String PREFIX = "Bearer ";
    public static final String COOKIE_NAME = "auth_token";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AuthenticatedUser user = jwtService.parse(token);
                var authorities = List.of(new SimpleGrantedAuthority("PLAN_" + user.plan()));
                var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ex) {
                // Trước đây nuốt lặng lẽ → 401 không rõ lý do. Log ở DEBUG (KHÔNG log giá trị token).
                log.debug("JWT bị từ chối ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.debug("Không có token: request KHÔNG kèm cookie nào (header có cookie? {})",
                    request.getHeader(HttpHeaders.COOKIE) != null);
            return null;
        }
        String value = Arrays.stream(cookies)
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
        if (value == null) {
            log.debug("Không có token: có {} cookie nhưng không có '{}' (tên: {})",
                    cookies.length, COOKIE_NAME, Arrays.stream(cookies).map(Cookie::getName).toList());
        }
        return value;
    }
}
