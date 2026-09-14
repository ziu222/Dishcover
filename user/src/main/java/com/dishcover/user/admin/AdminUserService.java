package com.dishcover.user.admin;

import com.dishcover.common.exception.ResourceNotFoundException;
import com.dishcover.user.admin.AdminUserDtos.AdminUserResponse;
import com.dishcover.user.entity.User;
import com.dishcover.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ quản trị tài khoản: xem, khoá/mở, đổi role.
 *
 * <p>Hai chốt an toàn quan trọng hơn mọi thứ khác ở đây: admin KHÔNG tự khoá và KHÔNG tự hạ quyền
 * chính mình. Thiếu chúng thì một cú bấm nhầm là mất quyền quản trị vĩnh viễn — phải vào tận RDS
 * chạy SQL mới cứu được, mà RDS lại để private đúng thiết kế.
 */
@Service
public class AdminUserService {

    private final UserRepository users;

    public AdminUserService(UserRepository users) {
        this.users = users;
    }

    /**
     * @param email chuỗi con của email để lọc; null/rỗng là lấy tất cả
     * @param pageable phân trang
     * @return trang user cho màn quản trị
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> list(String email, Pageable pageable) {
        return users.findByEmailContainingIgnoreCase(email == null ? "" : email.trim(), pageable)
                .map(AdminUserResponse::from);
    }

    /**
     * Khoá hoặc mở khoá tài khoản.
     *
     * @param actorId id admin đang thao tác, dùng để chặn tự khoá chính mình
     * @param targetId id tài khoản bị tác động
     * @param locked true = khoá
     * @return tài khoản sau khi đổi
     * @throws SelfTargetException nếu admin nhắm vào chính mình
     */
    @Transactional
    public AdminUserResponse setLocked(Long actorId, Long targetId, boolean locked) {
        User target = require(targetId);
        if (locked && actorId.equals(targetId)) {
            throw new SelfTargetException("Không thể tự khoá tài khoản của chính mình");
        }
        target.setLocked(locked);
        return AdminUserResponse.from(target);
    }

    /**
     * Đổi role của một tài khoản.
     *
     * @param actorId id admin đang thao tác
     * @param targetId id tài khoản bị tác động
     * @param role USER hoặc ADMIN (đã validate ở biên)
     * @return tài khoản sau khi đổi
     * @throws SelfTargetException nếu admin tự hạ quyền chính mình
     */
    @Transactional
    public AdminUserResponse setRole(Long actorId, Long targetId, String role) {
        User target = require(targetId);
        if (actorId.equals(targetId) && !"ADMIN".equals(role)) {
            throw new SelfTargetException("Không thể tự bỏ quyền quản trị của chính mình");
        }
        target.setRole(role);
        return AdminUserResponse.from(target);
    }

    private User require(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản " + id));
    }
}
