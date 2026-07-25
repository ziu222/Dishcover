package com.dishcover.user.service;

import com.dishcover.user.dto.DietaryDtos.DietaryPreferenceRequest;
import com.dishcover.user.dto.DietaryDtos.DietaryPreferenceResponse;
import com.dishcover.user.dto.UserResponse;
import com.dishcover.user.entity.DietaryPreference;
import com.dishcover.user.entity.User;
import com.dishcover.user.exception.ApiExceptions.ResourceNotFoundException;
import com.dishcover.user.repository.DietaryPreferenceRepository;
import com.dishcover.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final DietaryPreferenceRepository preferences;

    public UserService(UserRepository users, DietaryPreferenceRepository preferences) {
        this.users = users;
        this.preferences = preferences;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(requireUser(userId));
    }

    @Transactional(readOnly = true)
    public List<DietaryPreferenceResponse> listPreferences(Long userId) {
        return preferences.findByUserId(userId).stream()
                .map(DietaryPreferenceResponse::from)
                .toList();
    }

    @Transactional
    public DietaryPreferenceResponse addPreference(Long userId, DietaryPreferenceRequest req) {
        requireUser(userId);
        DietaryPreference saved = preferences.save(
                new DietaryPreference(userId, req.type(), req.value().trim()));
        return DietaryPreferenceResponse.from(saved);
    }

    @Transactional
    public void deletePreference(Long userId, Long preferenceId) {
        preferences.deleteByIdAndUserId(preferenceId, userId);
    }

    private User requireUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user id=" + userId));
    }
}
