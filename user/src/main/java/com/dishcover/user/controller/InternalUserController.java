package com.dishcover.user.controller;

import com.dishcover.common.exception.ResourceNotFoundException;
import com.dishcover.user.entity.User;
import com.dishcover.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Endpoint service-to-service DUY NHẤT không xác thực bằng JWT trong toàn hệ thống — lý do và
 * đánh đổi ghi ở specs/notification-service.md mục 5.3 và docs/superpowers/plans Stage 2. Route
 * Gateway là prefix match phẳng ({@code /user-service/internal/users/{id}} vẫn public từ Internet
 * như mọi route khác — CLAUDE.md mục 3 "Private Network" chỉ áp cho port container, không áp cho
 * path), nên PHẢI tự kiểm tra {@code X-Internal-Secret} ở đây, không được để permitAll trần.
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserRepository userRepository;
    private final String internalSecret;

    public InternalUserController(UserRepository userRepository,
                                   @Value("${internal.service-secret}") String internalSecret) {
        this.userRepository = userRepository;
        this.internalSecret = internalSecret;
    }

    @GetMapping("/{id}")
    public InternalUserResponse getById(@PathVariable Long id,
                                         @RequestHeader("X-Internal-Secret") String secret) {
        if (!MessageDigest.isEqual(internalSecret.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Internal secret không hợp lệ");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user id=" + id));
        return new InternalUserResponse(user.getId(), user.getEmail());
    }

    public record InternalUserResponse(Long id, String email) {
    }
}
