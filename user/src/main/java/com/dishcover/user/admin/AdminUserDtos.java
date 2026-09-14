package com.dishcover.user.admin;

import com.dishcover.user.entity.User;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

/** DTO cho khu quản trị user. Không bao giờ trả password_hash ra ngoài. */
public final class AdminUserDtos {

    private AdminUserDtos() {
    }

    public record AdminUserResponse(
            Long id,
            String email,
            String fullName,
            String role,
            boolean locked,
            boolean emailVerified,
            Instant createdAt
    ) {
        public static AdminUserResponse from(User u) {
            return new AdminUserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(),
                    u.getLocked(), u.getEmailVerified(), u.getCreatedAt());
        }
    }

    /** Khoá hoặc mở khoá tài khoản. */
    public record LockRequest(boolean locked) {
    }

    /** Đổi role. Chỉ nhận đúng 2 giá trị — chặn ngay ở biên thay vì để rác lọt xuống DB. */
    public record RoleRequest(
            @Pattern(regexp = "USER|ADMIN", message = "role phải là USER hoặc ADMIN") String role
    ) {
    }
}
