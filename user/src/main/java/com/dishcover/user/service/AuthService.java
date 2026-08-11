package com.dishcover.user.service;

import com.dishcover.common.security.JwtService;
import com.dishcover.user.dto.AuthDtos.AuthResponse;
import com.dishcover.user.dto.AuthDtos.LoginRequest;
import com.dishcover.user.dto.AuthDtos.RegisterRequest;
import com.dishcover.user.dto.UserResponse;
import com.dishcover.user.entity.User;
import com.dishcover.user.exception.ApiExceptions.EmailAlreadyExistsException;
import com.dishcover.user.exception.ApiExceptions.InvalidCredentialsException;
import com.dishcover.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer xử lý đăng ký/đăng nhập: chuẩn hóa email, băm/verify mật khẩu (BCrypt)
 * và phát hành JWT cho các luồng xác thực của User Service.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Đăng ký user mới với plan mặc định FREE.
     *
     * @param req thông tin đăng ký (email, password, fullName)
     * @return token JWT và thông tin user vừa tạo
     * @throws EmailAlreadyExistsException nếu email đã được đăng ký
     */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        User user = new User(email, passwordEncoder.encode(req.password()), req.fullName());
        users.save(user);
        return toAuthResponse(user);
    }

    /**
     * Xác thực email + mật khẩu và phát hành JWT nếu hợp lệ.
     *
     * @param req thông tin đăng nhập (email, password)
     * @return token JWT và thông tin user
     * @throws InvalidCredentialsException nếu email không tồn tại hoặc mật khẩu sai
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                jwtService.issue(user.getId(), user.getEmail(), user.getPlan()),
                jwtService.expirationSeconds(),
                UserResponse.from(user));
    }
}
