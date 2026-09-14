package com.dishcover.user.admin;

import com.dishcover.common.exception.ResourceNotFoundException;
import com.dishcover.user.admin.AdminUserDtos.AdminUserResponse;
import com.dishcover.user.entity.User;
import com.dishcover.user.repository.CalorieGoalRepository;
import com.dishcover.user.repository.DietaryPreferenceRepository;
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
    private final CalorieGoalRepository calorieGoals;
    private final DietaryPreferenceRepository preferences;
    private final UserDataCleaner cleaner;

    public AdminUserService(UserRepository users, CalorieGoalRepository calorieGoals,
                             DietaryPreferenceRepository preferences, UserDataCleaner cleaner) {
        this.users = users;
        this.calorieGoals = calorieGoals;
        this.preferences = preferences;
        this.cleaner = cleaner;
    }

    /**
     * Xoá hẳn một tài khoản và dọn dữ liệu của nó ở mọi service.
     *
     * <p>Thứ tự có chủ đích: dọn dữ liệu schema của chính mình, rồi gọi các service khác, và xoá
     * dòng {@code users} SAU CÙNG. Nếu xoá dòng users trước rồi một service lỗi, admin mất luôn
     * đầu mối trong danh sách để bấm dọn nốt — dữ liệu rác nằm lại vĩnh viễn mà không ai thấy.
     *
     * <p>Không saga, không bù trừ: mọi endpoint xoá đều idempotent nên bấm xoá lại là chạy tiếp.
     *
     * @param actorId admin đang thao tác
     * @param targetId tài khoản bị xoá
     * @return danh sách service chưa dọn được; rỗng nghĩa là sạch hết
     * @throws SelfTargetException nếu admin tự xoá chính mình
     */
    @Transactional
    public java.util.List<String> delete(Long actorId, Long targetId) {
        require(targetId);
        if (actorId.equals(targetId)) {
            throw new SelfTargetException("Không thể tự xoá tài khoản của chính mình");
        }
        calorieGoals.deleteByUserId(targetId);
        preferences.deleteByUserId(targetId);
        java.util.List<String> failed = cleaner.cleanRemote(targetId);
        if (failed.isEmpty()) {
            users.deleteById(targetId);
        }
        return failed;
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
