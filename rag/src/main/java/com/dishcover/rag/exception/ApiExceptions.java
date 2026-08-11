package com.dishcover.rag.exception;

/** Namespace các exception nghiệp vụ riêng của RAG Service, ánh xạ mã HTTP qua {@link GlobalExceptionHandler}. */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 503 — Matching/User Service không phản hồi được sau khi circuit breaker fallback. */
    public static class UpstreamUnavailableException extends RuntimeException {

        /** @param message thông báo lỗi trả về client */
        public UpstreamUnavailableException(String message) {
            super(message);
        }

        /** Giữ nguyên causal chain (VD JWT_SECRET lệch giữa service) — log/debug không bị mất manh mối. */
        public UpstreamUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
