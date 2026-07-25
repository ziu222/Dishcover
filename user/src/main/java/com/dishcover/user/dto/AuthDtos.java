package com.dishcover.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO cho luồng đăng ký/đăng nhập. Tách khỏi entity — không expose password_hash ra API. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 6, max = 72) String password,
            @Size(max = 100) String fullName
    ) {
    }

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
