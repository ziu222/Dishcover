package com.dishcover.user.service;

import com.dishcover.user.dto.DietaryDtos.DietaryPreferenceRequest;
import com.dishcover.user.dto.DietaryDtos.DietaryPreferenceResponse;
import com.dishcover.user.dto.UpdateProfileRequest;
import com.dishcover.user.dto.UserResponse;
import com.dishcover.user.entity.DietaryPreference;
import com.dishcover.user.entity.User;
import com.dishcover.common.exception.ResourceNotFoundException;
import com.dishcover.user.repository.DietaryPreferenceRepository;
import com.dishcover.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer xử lý hồ sơ user và hồ sơ ăn uống (dietary preferences): đọc/ghi qua
 * {@link UserRepository} và {@link DietaryPreferenceRepository}.
 */
@Service
public class UserService {

    private final UserRepository users;
    private final DietaryPreferenceRepository preferences;

    public UserService(UserRepository users, DietaryPreferenceRepository preferences) {
        this.users = users;
        this.preferences = preferences;
    }

    /**
     * Lấy hồ sơ của một user.
     *
     * @param userId id user cần lấy
     * @return thông tin hồ sơ user
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu user không tồn tại
     */
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(requireUser(userId));
    }

    /**
     * Cập nhật một phần hồ sơ user (họ tên, avatar). Field null trong {@code req} giữ nguyên giá
     * trị cũ, chỉ field khác null mới được ghi — không cho sửa email/password ở đây.
     *
     * @param userId id user cần cập nhật
     * @param req    field cần cập nhật
     * @return hồ sơ user sau khi cập nhật
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu user không tồn tại
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = requireUser(userId);
        if (req.fullName() != null) {
            user.setFullName(req.fullName());
        }
        if (req.avatarUrl() != null) {
            user.setAvatarUrl(req.avatarUrl());
        }
        return UserResponse.from(user);
    }

    /**
     * Liệt kê toàn bộ hồ sơ ăn uống của một user.
     *
     * @param userId id user cần lấy
     * @return danh sách mục hồ sơ ăn uống, rỗng nếu chưa có
     */
    @Transactional(readOnly = true)
    public List<DietaryPreferenceResponse> listPreferences(Long userId) {
        return preferences.findByUserId(userId).stream()
                .map(DietaryPreferenceResponse::from)
                .toList();
    }

    /**
     * Thêm một mục hồ sơ ăn uống mới cho user.
     *
     * @param userId id user sở hữu mục cần thêm
     * @param req    thông tin mục cần thêm (type, value)
     * @return mục hồ sơ ăn uống vừa tạo
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu user không tồn tại
     */
    @Transactional
    public DietaryPreferenceResponse addPreference(Long userId, DietaryPreferenceRequest req) {
        requireUser(userId);
        DietaryPreference saved = preferences.save(
                new DietaryPreference(userId, req.type(), req.value().trim()));
        return DietaryPreferenceResponse.from(saved);
    }

    /**
     * Xóa một mục hồ sơ ăn uống, chỉ khi thuộc đúng user (chống xóa hộ user khác).
     *
     * @param userId       id user sở hữu mục cần xóa
     * @param preferenceId id mục cần xóa
     * @throws ResourceNotFoundException nếu id không tồn tại hoặc không thuộc user này
     */
    @Transactional
    public void deletePreference(Long userId, Long preferenceId) {
        if (preferences.deleteByIdAndUserId(preferenceId, userId) == 0) {
            throw new ResourceNotFoundException("Không tìm thấy mục hồ sơ ăn uống id=" + preferenceId);
        }
    }

    private User requireUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user id=" + userId));
    }
}
