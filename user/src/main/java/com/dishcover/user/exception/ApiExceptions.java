package com.dishcover.user.exception;

/** Exception nghiệp vụ, map sang mã HTTP ở GlobalExceptionHandler. */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 409 — email đã đăng ký. */
    public static class EmailAlreadyExistsException extends RuntimeException {
        /**
         * @param email email đã tồn tại trong hệ thống, được nhúng vào thông báo lỗi
         */
        public EmailAlreadyExistsException(String email) {
            super("Email đã được đăng ký: " + email);
        }
    }

    /** 401 — sai email hoặc mật khẩu. */
    public static class InvalidCredentialsException extends RuntimeException {
        /** Tạo exception với thông báo mặc định "Email hoặc mật khẩu không đúng". */
        public InvalidCredentialsException() {
            super("Email hoặc mật khẩu không đúng");
        }
    }

    /** 429 — sai đủ {@link com.dishcover.user.security.LoginAttemptTracker#LOCK_THRESHOLD} lần, tạm khoá theo email. */
    public static class TooManyAttemptsException extends RuntimeException {
        public TooManyAttemptsException() {
            super("Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau ít phút.");
        }
    }

    /** 422 — đã sai đủ ngưỡng CAPTCHA, thiếu hoặc sai captchaToken. */
    public static class CaptchaRequiredException extends RuntimeException {
        public CaptchaRequiredException() {
            super("Vui lòng xác minh CAPTCHA trước khi đăng nhập lại.");
        }
    }

    /** 503 — gửi email OTP thất bại (SMTP lỗi/timeout). Khác Notification Service: đây KHÔNG
     *  best-effort — email OTP là sản phẩm chính của luồng đăng ký, không phải nhắc nhở phụ. */
    public static class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message) {
            super(message);
        }
    }

    /** 422 — mã OTP nhập sai. */
    public static class InvalidOtpException extends RuntimeException {
        public InvalidOtpException() {
            super("Mã xác thực không đúng.");
        }
    }

    /** 422 — mã OTP đã hết hạn hoặc chưa từng được sinh cho email này (2 trường hợp gộp
     *  chung 1 exception vì cùng dẫn tới 1 hành động: bấm "Gửi lại"). */
    public static class OtpExpiredException extends RuntimeException {
        public OtpExpiredException() {
            super("Mã xác thực đã hết hạn hoặc không tồn tại. Vui lòng bấm Gửi lại.");
        }
    }

    /** 429 — gọi resend-otp quá sớm, còn trong cửa sổ cooldown. */
    public static class TooSoonException extends RuntimeException {
        public TooSoonException(long secondsRemaining) {
            super("Vui lòng đợi " + secondsRemaining + " giây trước khi gửi lại.");
        }
    }

    /** 403 — đăng nhập đúng mật khẩu nhưng email chưa xác thực OTP. */
    public static class EmailNotVerifiedException extends RuntimeException {
        public EmailNotVerifiedException() {
            super("Email chưa được xác thực. Vui lòng kiểm tra hộp thư.");
        }
    }

    /** Tài khoản bị quản trị viên khoá — chặn đăng nhập. */
    public static class AccountLockedException extends RuntimeException {
        public AccountLockedException() {
            super("Tài khoản đã bị khoá. Liên hệ quản trị viên để được mở lại.");
        }
    }
}
