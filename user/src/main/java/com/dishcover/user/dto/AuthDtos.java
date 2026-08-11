package com.dishcover.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO cho luồng đăng ký/đăng nhập. Tách khỏi entity — không expose password_hash ra API. */
public final class AuthDtos {

    private AuthDtos() {
    }

    /**
     * Yêu cầu đăng ký tài khoản mới.
     *
     * @param email    email đăng nhập, phải là duy nhất trong hệ thống
     * @param password mật khẩu dạng plaintext gửi lên, được băm (BCrypt) trước khi lưu
     * @param fullName họ tên hiển thị, có thể để trống
     */
    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 6, max = 72) String password,
            @Size(max = 100) String fullName
    ) {
    }

    /**
     * Yêu cầu đăng nhập bằng email + mật khẩu.
     *
     * @param password mật khẩu dạng plaintext gửi lên, so khớp với password_hash đã lưu
     */
    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    /** Trả token + thông tin cơ bản để client hiển thị ngay, khỏi gọi thêm /users/me. */
    public record AuthResponse(
            String token,
            long expiresInSeconds,
            UserResponse user
    ) {
    }
}
