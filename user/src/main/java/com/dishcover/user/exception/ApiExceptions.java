package com.dishcover.user.exception;

/** Exception nghiệp vụ, map sang mã HTTP ở GlobalExceptionHandler. */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 409 — email đã đăng ký. */
    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String email) {
            super("Email đã được đăng ký: " + email);
        }
    }

    /** 401 — sai email hoặc mật khẩu. */
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Email hoặc mật khẩu không đúng");
        }
    }
}
