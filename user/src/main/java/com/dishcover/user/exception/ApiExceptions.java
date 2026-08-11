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
}
