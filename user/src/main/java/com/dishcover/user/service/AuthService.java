package com.dishcover.user.service;

import com.dishcover.common.security.JwtService;
import com.dishcover.user.dto.AuthDtos.AuthResult;
import com.dishcover.user.dto.AuthDtos.LoginRequest;
import com.dishcover.user.dto.AuthDtos.RegisterRequest;
import com.dishcover.user.dto.AuthDtos.ResendOtpRequest;
import com.dishcover.user.dto.AuthDtos.VerifyOtpRequest;
import com.dishcover.user.dto.UserResponse;
import com.dishcover.user.entity.User;
import com.dishcover.user.exception.ApiExceptions.AccountLockedException;
import com.dishcover.user.exception.ApiExceptions.EmailAlreadyExistsException;
import com.dishcover.user.exception.ApiExceptions.EmailNotVerifiedException;
import com.dishcover.user.exception.ApiExceptions.InvalidCredentialsException;
import com.dishcover.user.exception.ApiExceptions.InvalidOtpException;
import com.dishcover.user.exception.ApiExceptions.OtpExpiredException;
import com.dishcover.user.mail.EmailSender;
import com.dishcover.user.repository.UserRepository;
import com.dishcover.user.security.OtpStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer xử lý đăng ký/đăng nhập/xác thực OTP: chuẩn hóa email, băm/verify mật khẩu
 * (BCrypt) và phát hành JWT cho các luồng xác thực của User Service.
 *
 * <p><b>Luồng đăng ký mới (docs/specs/email-otp-verification.md):</b> {@code register()} KHÔNG
 * còn phát JWT — chỉ tạo user ({@code emailVerified=false}) + gửi OTP. JWT chỉ phát hành sau
 * {@link #verifyOtp} đúng mã, y hệt {@link #login}.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpStore otpStore;
    private final EmailSender emailSender;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                        OtpStore otpStore, EmailSender emailSender) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpStore = otpStore;
        this.emailSender = emailSender;
    }

    /**
     * Đăng ký user mới với plan mặc định FREE, {@code emailVerified=false}, gửi mã OTP qua email.
     * KHÔNG phát JWT — user phải gọi {@link #verifyOtp} đúng mã mới đăng nhập được.
     *
     * @param req thông tin đăng ký (email, password, fullName)
     * @throws EmailAlreadyExistsException nếu email đã được đăng ký
     * @throws com.dishcover.user.exception.ApiExceptions.EmailDeliveryException nếu gửi email lỗi
     *         (user vẫn được tạo trong DB — bấm "Gửi lại" ở bước sau sẽ retry gửi được)
     */
    @Transactional
    public void register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        User user = new User(email, passwordEncoder.encode(req.password()), req.fullName());
        users.save(user);
        String otp = otpStore.issue(email);
        emailSender.send(email, otp);
    }

    /**
     * Xác thực mã OTP, đánh dấu email đã xác thực và phát hành JWT nếu đúng.
     *
     * @param req email + mã OTP người dùng nhập
     * @return token JWT và thông tin user
     * @throws InvalidCredentialsException nếu email không tồn tại (không nên xảy ra bình thường —
     *         chỉ khi user bị xoá giữa lúc đăng ký và verify)
     * @throws InvalidOtpException nếu sai mã
     * @throws OtpExpiredException nếu mã hết hạn hoặc không tồn tại
     */
    @Transactional
    public AuthResult verifyOtp(VerifyOtpRequest req) {
        String email = req.email().trim().toLowerCase();
        OtpStore.VerifyResult result = otpStore.verify(email, req.otp());
        switch (result) {
            case WRONG -> throw new InvalidOtpException();
            case EXPIRED, NOT_FOUND -> throw new OtpExpiredException();
            case OK -> { /* tiếp tục bên dưới */ }
        }
        User user = users.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        user.setEmailVerified(true);
        users.save(user);
        otpStore.clear(email);
        return toAuthResponse(user);
    }

    /**
     * Gửi lại mã OTP mới. KHÔNG lộ thông tin email nào đã đăng ký (chống user-enumeration) —
     * email không tồn tại hoặc đã xác thực rồi đều trả về bình thường, không ném lỗi.
     *
     * @param req email cần gửi lại mã
     * @throws com.dishcover.user.exception.ApiExceptions.TooSoonException nếu còn trong cửa sổ
     *         cooldown 60s của lần gửi trước
     * @throws com.dishcover.user.exception.ApiExceptions.EmailDeliveryException nếu gửi email lỗi
     */
    public void resendOtp(ResendOtpRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = users.findByEmail(email).orElse(null);
        if (user == null || user.getEmailVerified()) {
            return; // im lặng — không lộ "email này có tồn tại/đã verified hay chưa"
        }
        String otp = otpStore.issue(email);
        emailSender.send(email, otp);
    }

    /**
     * Xác thực email + mật khẩu và phát hành JWT nếu hợp lệ.
     *
     * @param req thông tin đăng nhập (email, password)
     * @return token JWT và thông tin user
     * @throws InvalidCredentialsException nếu email không tồn tại hoặc mật khẩu sai
     * @throws EmailNotVerifiedException nếu email chưa xác thực OTP (đặt SAU bước check password —
     *         không lộ "email này đã đăng ký nhưng chưa verify" cho ai gõ đúng email sai password)
     */
    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest req) {
        User user = users.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.getEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
        // Dat SAU buoc check mat khau, cung ly do voi EmailNotVerified: khong lo "tai khoan nay
        // ton tai va dang bi khoa" cho nguoi go sai mat khau.
        if (user.getLocked()) {
            throw new AccountLockedException();
        }
        return toAuthResponse(user);
    }

    private AuthResult toAuthResponse(User user) {
        return new AuthResult(
                jwtService.issue(user.getId(), user.getEmail(), user.getPlan(), user.getRole()),
                jwtService.expirationSeconds(),
                UserResponse.from(user));
    }
}
