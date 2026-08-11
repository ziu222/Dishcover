package com.dishcover.user.controller;

import com.dishcover.user.dto.AuthDtos.AuthResponse;
import com.dishcover.user.dto.AuthDtos.LoginRequest;
import com.dishcover.user.dto.AuthDtos.RegisterRequest;
import com.dishcover.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller cho luồng xác thực (đăng ký/đăng nhập) của User Service.
 * Công khai (không cần JWT) — xem {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Đăng ký tài khoản mới.
     *
     * @param req thông tin đăng ký (email, password, fullName)
     * @return 201 Created kèm token JWT và thông tin user vừa tạo
     * @throws com.dishcover.user.exception.ApiExceptions.EmailAlreadyExistsException nếu email đã được đăng ký
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    /**
     * Đăng nhập bằng email + mật khẩu.
     *
     * @param req thông tin đăng nhập (email, password)
     * @return token JWT và thông tin user
     * @throws com.dishcover.user.exception.ApiExceptions.InvalidCredentialsException nếu sai email hoặc mật khẩu
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
