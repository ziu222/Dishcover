package com.dishcover.user.service;

import com.dishcover.user.dto.CalorieGoalDtos.CalorieGoalRequest;
import com.dishcover.user.dto.CalorieGoalDtos.CalorieGoalResponse;
import com.dishcover.user.dto.DietaryDtos.DietaryPreferenceRequest;
import com.dishcover.user.dto.DietaryDtos.DietaryPreferenceResponse;
import com.dishcover.user.dto.UpdateProfileRequest;
import com.dishcover.user.dto.UserResponse;
import com.dishcover.user.entity.CalorieGoal;
import com.dishcover.user.entity.DietaryPreference;
import com.dishcover.user.entity.User;
import com.dishcover.common.exception.ResourceNotFoundException;
import com.dishcover.user.repository.CalorieGoalRepository;
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
    private final CalorieGoalRepository calorieGoals;

    public UserService(UserRepository users, DietaryPreferenceRepository preferences,
                        CalorieGoalRepository calorieGoals) {
        this.users = users;
        this.preferences = preferences;
        this.calorieGoals = calorieGoals;
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
     * Thêm một mục hồ sơ ăn uống mới cho user — idempotent theo (user_id, type, value): bấm
     * "thêm" nhiều lần cho cùng giá trị trả về đúng mục đã có, không tạo dòng trùng (bug thật
     * phát hiện lúc live-verify, xem V3__dietary_preferences_unique.sql).
     *
     * @param userId id user sở hữu mục cần thêm
     * @param req    thông tin mục cần thêm (type, value)
     * @return mục hồ sơ ăn uống đã có hoặc vừa tạo
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu user không tồn tại
     */
    @Transactional
    public DietaryPreferenceResponse addPreference(Long userId, DietaryPreferenceRequest req) {
        requireUser(userId);
        String value = req.value().trim();
        DietaryPreference existing = preferences.findByUserIdAndTypeAndValue(userId, req.type(), value)
                .orElse(null);
        if (existing != null) {
            return DietaryPreferenceResponse.from(existing);
        }
        DietaryPreference saved = preferences.save(new DietaryPreference(userId, req.type(), value));
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

    /**
     * Lấy mục tiêu calo/macro/ngày hiện tại của user, nếu có.
     *
     * @param userId id user cần lấy
     * @return mục tiêu hiện tại, {@code null} nếu user chưa đặt mục tiêu (opt-in, không ảnh hưởng
     *         hành vi những nơi dùng nếu không set — Recipe Detail/Matching Rule coi là "chưa đặt")
     */
    @Transactional(readOnly = true)
    public CalorieGoalResponse getCalorieGoal(Long userId) {
        return calorieGoals.findByUserId(userId).map(CalorieGoalResponse::from).orElse(null);
    }

    /**
     * Đặt/sửa mục tiêu calo/macro/ngày — upsert theo user (mỗi user tối đa 1 mục tiêu).
     *
     * @param userId id user sở hữu mục tiêu
     * @param req    4 con số mục tiêu mới
     * @return mục tiêu sau khi lưu
     */
    @Transactional
    public CalorieGoalResponse upsertCalorieGoal(Long userId, CalorieGoalRequest req) {
        CalorieGoal goal = calorieGoals.findByUserId(userId).orElse(null);
        if (goal == null) {
            goal = calorieGoals.save(new CalorieGoal(
                    userId, req.calorieTarget(), req.proteinTarget(), req.carbTarget(), req.fatTarget()));
        } else {
            goal.update(req.calorieTarget(), req.proteinTarget(), req.carbTarget(), req.fatTarget());
        }
        return CalorieGoalResponse.from(goal);
    }

    private User requireUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user id=" + userId));
    }
}
