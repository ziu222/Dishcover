package com.dishcover.user.dto;

import com.dishcover.user.entity.User;

/** Thông tin user an toàn để trả ra API — KHÔNG chứa password_hash. */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String avatarUrl,
        String plan
) {
    /**
     * Chuyển entity {@link User} sang DTO an toàn để trả ra API.
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
                user.getPlan());
    }
}
