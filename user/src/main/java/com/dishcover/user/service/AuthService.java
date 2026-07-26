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
