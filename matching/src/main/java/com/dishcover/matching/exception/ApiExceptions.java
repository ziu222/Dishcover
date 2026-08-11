package com.dishcover.matching.exception;

/** Gom nhóm các exception nghiệp vụ riêng của Matching Service (namespace, không khởi tạo được). */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 503 — Inventory/Recipe/User Service không phản hồi được sau khi circuit breaker fallback. */
    public static class UpstreamUnavailableException extends RuntimeException {
        /**
         * @param message thông báo lỗi trả về client, mô tả service ngoài nào đang không khả dụng
         */
        public UpstreamUnavailableException(String message) {
            super(message);
        }
    }
}
