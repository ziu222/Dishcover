package com.dishcover.user.controller;

import com.dishcover.common.security.JwtAuthFilter;
import com.dishcover.user.dto.AuthDtos.AuthResult;
import com.dishcover.user.dto.AuthDtos.LoginRequest;
import com.dishcover.user.dto.AuthDtos.RegisterRequest;
import com.dishcover.user.dto.AuthDtos.ResendOtpRequest;
import com.dishcover.user.dto.AuthDtos.VerifyOtpRequest;
import com.dishcover.user.dto.UserResponse;
import com.dishcover.user.exception.ApiExceptions.CaptchaRequiredException;
import com.dishcover.user.exception.ApiExceptions.InvalidCredentialsException;
import com.dishcover.user.exception.ApiExceptions.TooManyAttemptsException;
import com.dishcover.user.security.LoginAttemptTracker;
import com.dishcover.user.security.TurnstileClient;
import com.dishcover.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller cho luồng xác thực (đăng ký/xác thực OTP/đăng nhập/đăng xuất) của User Service.
 * Công khai (không cần JWT) — xem {@code SecurityConfig}.
 *
 * Token phát ra qua cookie httpOnly (JS không đọc được, hạn chế rủi ro XSS đánh cắp token),
 * không còn trả trong JSON body — body chỉ trả {@link UserResponse} để client hiển thị ngay,
 * cùng shape với {@code GET /users/me} nên frontend dùng lại y hệt logic sau khi xác thực.
 *
 * <p><b>Luồng mới (docs/specs/email-otp-verification.md):</b> {@code register()} KHÔNG còn set
 * cookie — chỉ báo "đã gửi OTP". Chỉ {@link #verifyOtp} và {@link #login} mới phát cookie.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /**
     * SameSite=Lax là đủ chống CSRF cho kiến trúc hiện tại (frontend/Gateway cùng site qua
     * Vite proxy ở dev, giả định cùng site ở prod) — cookie không gửi kèm request cross-site
     * không phải điều hướng top-level. Nếu sau này frontend/Gateway tách domain thật, cần đổi
     * sang SameSite=None + Secure + CORS credentials tường minh.
     */
    private static final String SAME_SITE = "Lax";

    private final AuthService authService;
    private final LoginAttemptTracker attemptTracker;
    private final TurnstileClient turnstileClient;

    /** secure=false ở dev (http://localhost) — cookie Secure bị trình duyệt chặn nếu không có HTTPS. */
    @Value("${app.cookie-secure:false}")
    private boolean cookieSecure;

    public AuthController(AuthService authService, LoginAttemptTracker attemptTracker,
                           TurnstileClient turnstileClient) {
        this.authService = authService;
        this.attemptTracker = attemptTracker;
        this.turnstileClient = turnstileClient;
    }

    /**
     * Đăng ký tài khoản mới — tạo user chưa xác thực, gửi mã OTP qua email. KHÔNG đặt cookie —
     * phải gọi {@link #verifyOtp} đúng mã mới đăng nhập được.
     *
     * @param req payload đăng ký (email, password, fullName)
     * @return 201 Created kèm thông báo đã gửi OTP
     * @throws com.dishcover.user.exception.ApiExceptions.EmailAlreadyExistsException nếu email đã được đăng ký
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Đã gửi mã xác thực tới " + req.email().trim().toLowerCase()));
    }

    /**
     * Xác thực mã OTP đã gửi qua email lúc đăng ký — đúng mã thì đánh dấu email đã xác thực và
     * phát JWT y hệt {@link #login}.
     *
     * @param req email + mã OTP
     * @return hồ sơ user, token đặt qua cookie httpOnly
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<UserResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        AuthResult result = authService.verifyOtp(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie(result).toString())
                .body(result.user());
    }

    /**
     * Gửi lại mã OTP mới. Luôn trả 200 (kể cả email không tồn tại/đã xác thực rồi) — không lộ
     * thông tin email nào đã đăng ký.
     *
     * @param req email cần gửi lại mã
     * @return 200 rỗng
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@Valid @RequestBody ResendOtpRequest req) {
        authService.resendOtp(req);
        return ResponseEntity.ok().build();
    }

    /**
     * Đăng nhập bằng email + mật khẩu. Bảo vệ bởi {@link LoginAttemptTracker} (Fixed Window
     * Counter theo email): sai từ {@value LoginAttemptTracker#CAPTCHA_THRESHOLD} lần trở lên
     * trong cửa sổ phải kèm {@code captchaToken} hợp lệ mới cho thử tiếp; sai đủ
     * {@value LoginAttemptTracker#LOCK_THRESHOLD} lần thì khoá cứng, từ chối thẳng không chạm
     * DB/bcrypt. Đăng nhập đúng reset bộ đếm ngay.
     *
     * @param req  payload đăng nhập (email, password, captchaToken tuỳ ngưỡng)
     * @param http để lấy IP người dùng gửi kèm lúc verify CAPTCHA (chỉ để chấm điểm, không bắt buộc)
     * @return hồ sơ user, token đặt qua cookie httpOnly
     * @throws TooManyAttemptsException     nếu email đã bị khoá tạm thời
     * @throws CaptchaRequiredException     nếu đến ngưỡng cần CAPTCHA mà token thiếu/sai
     * @throws InvalidCredentialsException  nếu sai email hoặc mật khẩu
     * @throws com.dishcover.user.exception.ApiExceptions.EmailNotVerifiedException nếu email chưa xác thực OTP
     */
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        String email = req.email().trim().toLowerCase();

        LoginAttemptTracker.Status status = attemptTracker.status(email);
        if (status == LoginAttemptTracker.Status.LOCKED) {
            throw new TooManyAttemptsException();
        }
        if (status == LoginAttemptTracker.Status.NEEDS_CAPTCHA
                && !turnstileClient.verify(req.captchaToken(), http.getRemoteAddr())) {
            throw new CaptchaRequiredException();
        }

        AuthResult result;
        try {
            result = authService.login(req);
        } catch (InvalidCredentialsException ex) {
            attemptTracker.recordFailure(email);
            throw ex;
        }
        attemptTracker.reset(email);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie(result).toString())
                .body(result.user());
    }

    /**
     * Đăng xuất — xoá cookie phía trình duyệt. Không có state phía server để dọn (JWT
     * stateless, không lưu session/token blacklist), nên chỉ cần hết hạn cookie ngay lập tức.
     *
     * @return 204 No Content kèm cookie hết hạn
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie expired = ResponseCookie.from(JwtAuthFilter.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(SAME_SITE)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .build();
    }

    private ResponseCookie authCookie(AuthResult result) {
        return ResponseCookie.from(JwtAuthFilter.COOKIE_NAME, result.token())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(SAME_SITE)
                .path("/")
                .maxAge(result.expiresInSeconds())
                .build();
    }
}
