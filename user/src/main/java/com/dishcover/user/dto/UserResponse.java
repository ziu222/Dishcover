package com.dishcover.user.dto;

import com.dishcover.user.entity.User;

/** Thông tin user an toàn để trả ra API — KHÔNG chứa password_hash. */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String avatarUrl,
        String plan,
        String role
) {
    /**
     * Chuyển entity {@link User} sang DTO an toàn để trả ra API. {@code role} chỉ dùng để frontend
     * gate hiển thị (VD link admin) — quyền thật luôn được backend kiểm tra lại qua JWT, không dựa
     * vào giá trị client đọc được ở đây.
     *
     * @param user entity user nguồn
     * @return DTO tương ứng, không chứa password_hash
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getPlan(),
                user.getRole());
    }
}
