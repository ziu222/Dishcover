package com.dishcover.common.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * Lấy chuỗi {@code "Bearer <token>"} để FORWARD JWT của người dùng sang service downstream
 * (Inventory/Recipe/User). Ưu tiên header {@code Authorization}; nếu không có (trình duyệt dùng
 * httpOnly cookie, không gửi được header) thì lấy từ cookie {@link JwtAuthFilter#COOKIE_NAME}.
 *
 * <p>Matching/RAG trước đây nhận token qua {@code @RequestHeader("Authorization")} bắt buộc nên
 * KHÔNG gọi được từ frontend (chỉ có cookie). Helper này gỡ ràng buộc đó, khớp cùng nguồn token
 * với {@link JwtAuthFilter}.</p>
 */
public final class RequestTokenExtractor {

    private static final String BEARER = "Bearer ";

    private RequestTokenExtractor() {
    }

    /** Trả {@code "Bearer <token>"} từ header hoặc cookie; {@code null} nếu request không kèm token. */
    public static String resolveBearer(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            return header;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (JwtAuthFilter.COOKIE_NAME.equals(c.getName())) {
                    return BEARER + c.getValue();
                }
            }
        }
        return null;
    }
}
