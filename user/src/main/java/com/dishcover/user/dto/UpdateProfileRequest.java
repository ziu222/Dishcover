package com.dishcover.user.dto;

import jakarta.validation.constraints.Size;

/**
 * Cập nhật một phần (partial update) hồ sơ user — field null nghĩa là giữ nguyên giá trị cũ,
 * chỉ field khác null mới được ghi. Không cho sửa email/password ở đây (đổi mật khẩu/email cần
 * luồng xác thực riêng, không nằm trong phạm vi màn Tài khoản hiện tại).
 *
 * @param fullName  họ tên hiển thị mới, null nếu không đổi
 * @param avatarUrl URL ảnh đại diện mới, null nếu không đổi
 */
public record UpdateProfileRequest(
        @Size(max = 100) String fullName,
        @Size(max = 2048) String avatarUrl
) {
}
