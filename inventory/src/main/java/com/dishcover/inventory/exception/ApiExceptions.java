package com.dishcover.inventory.exception;

/** Exception nghiệp vụ, map sang mã HTTP ở GlobalExceptionHandler. */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 404 — không tìm thấy tài nguyên, hoặc tồn tại nhưng không thuộc user hiện tại (không lộ 403). */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}
