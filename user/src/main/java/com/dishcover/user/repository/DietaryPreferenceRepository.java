package com.dishcover.user.repository;

import com.dishcover.user.entity.DietaryPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository JPA cho entity {@link DietaryPreference}, thao tác trực tiếp bảng
 * user_service.dietary_preferences.
 */
public interface DietaryPreferenceRepository extends JpaRepository<DietaryPreference, Long> {

    /**
     * Liệt kê toàn bộ mục hồ sơ ăn uống của một user.
     *
     * @param userId id user cần lấy
     * @return danh sách mục hồ sơ ăn uống, rỗng nếu không có
     */
    List<DietaryPreference> findByUserId(Long userId);

    /**
     * Xóa một mục hồ sơ ăn uống theo id, chỉ khi thuộc đúng user (chống xóa hộ user khác).
     *
     * @param id     id mục cần xóa
     * @param userId id user sở hữu mục cần xóa
     */
    void deleteByIdAndUserId(Long id, Long userId);
}
