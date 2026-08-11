package com.dishcover.user.controller;

import com.dishcover.user.dto.DietaryDtos.DietaryPreferenceRequest;
import com.dishcover.user.dto.DietaryDtos.DietaryPreferenceResponse;
import com.dishcover.user.dto.UserResponse;
import com.dishcover.common.security.AuthenticatedUser;
import com.dishcover.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoint hồ sơ người dùng — userId luôn lấy từ JWT, không nhận từ client (tránh giả mạo). */
@RestController
@RequestMapping("/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lấy hồ sơ của user đang đăng nhập.
     *
     * @param me user đã xác thực, suy ra từ JWT
     * @return thông tin hồ sơ user
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu user không còn tồn tại
     */
    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser me) {
        return userService.getProfile(me.userId());
    }

    /**
     * Liệt kê toàn bộ hồ sơ ăn uống (dị ứng/chế độ ăn) của user đang đăng nhập.
     *
     * @param me user đã xác thực, suy ra từ JWT
     * @return danh sách mục hồ sơ ăn uống, rỗng nếu chưa có
     */
    @GetMapping("/dietary-preferences")
    public List<DietaryPreferenceResponse> listPreferences(@AuthenticationPrincipal AuthenticatedUser me) {
        return userService.listPreferences(me.userId());
    }

    /**
     * Thêm một mục hồ sơ ăn uống mới cho user đang đăng nhập.
     *
     * @param me  user đã xác thực, suy ra từ JWT
     * @param req thông tin mục cần thêm (type, value)
     * @return 201 Created kèm mục vừa tạo
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu user không còn tồn tại
     */
    @PostMapping("/dietary-preferences")
    public ResponseEntity<DietaryPreferenceResponse> addPreference(
            @AuthenticationPrincipal AuthenticatedUser me,
            @Valid @RequestBody DietaryPreferenceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.addPreference(me.userId(), req));
    }

    /**
     * Xóa một mục hồ sơ ăn uống của user đang đăng nhập.
     *
     * @param me user đã xác thực, suy ra từ JWT
     * @param id id mục cần xóa
     */
    @DeleteMapping("/dietary-preferences/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePreference(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        userService.deletePreference(me.userId(), id);
    }
}
