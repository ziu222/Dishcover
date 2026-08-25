package com.dishcover.user.controller;

import com.dishcover.common.security.JwtAuthFilter;
import com.dishcover.user.dto.AuthDtos.AuthResult;
import com.dishcover.user.dto.AuthDtos.LoginRequest;
import com.dishcover.user.dto.AuthDtos.RegisterRequest;
import com.dishcover.user.dto.UserResponse;
import com.dishcover.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller cho luồng xác thực (đăng ký/đăng nhập/đăng xuất) của User Service.
 * Công khai (không cần JWT) — xem {@code SecurityConfig}.
 *
 * Token phát ra qua cookie httpOnly (JS không đọc được, hạn chế rủi ro XSS đánh cắp token),
 * không còn trả trong JSON body — body chỉ trả {@link UserResponse} để client hiển thị ngay,
 * cùng shape với {@code GET /users/me} nên frontend dùng lại y hệt logic sau khi xác thực.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /**
     * SameSite=Lax là đủ chống CSRF cho kiến trúc hiện tại (frontend/Gateway cùng site qua
     * Vite proxy ở dev, giả định cùng site ở prod) — cookie không gửi kèm request cross-site
     * không phải điều hướng top-level. Nếu sau này frontend/Gateway tách domain thật, cần đổi
     * sang SameSite=None + Secure + CORS credentials tường minh.
     */
    private static final String SAME_SITE = "Lax";

    private final AuthService authService;

    /** secure=false ở dev (http://localhost) — cookie Secure bị trình duyệt chặn nếu không có HTTPS. */
    @Value("${app.cookie-secure:false}")
    private boolean cookieSecure;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Đăng ký tài khoản mới.
     *
     * @param req payload đăng ký (email, password, fullName)
     * @return 201 Created kèm hồ sơ user vừa tạo, token đặt qua cookie httpOnly
     * @throws com.dishcover.user.exception.ApiExceptions.EmailAlreadyExistsException nếu email đã được đăng ký
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        AuthResult result = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, authCookie(result).toString())
                .body(result.user());
    }

    /**
     * Đăng nhập bằng email + mật khẩu.
     *
     * @param req payload đăng nhập (email, password)
     * @return hồ sơ user, token đặt qua cookie httpOnly
     * @throws com.dishcover.user.exception.ApiExceptions.InvalidCredentialsException nếu sai email hoặc mật khẩu
     */
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthResult result = authService.login(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie(result).toString())
                .body(result.user());
    }

    /**
     * Đăng xuất — xoá cookie phía trình duyệt. Không có state phía server để dọn (JWT
     * stateless, không lưu session/token blacklist), nên chỉ cần hết hạn cookie ngay lập tức.
     *
     * @return 204 No Content kèm cookie hết hạn
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie expired = ResponseCookie.from(JwtAuthFilter.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(SAME_SITE)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .build();
    }

    private ResponseCookie authCookie(AuthResult result) {
        return ResponseCookie.from(JwtAuthFilter.COOKIE_NAME, result.token())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(SAME_SITE)
                .path("/")
                .maxAge(result.expiresInSeconds())
                .build();
    }
}
