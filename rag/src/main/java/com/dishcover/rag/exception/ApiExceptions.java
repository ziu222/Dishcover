package com.dishcover.rag.exception;

public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 503 — Matching/User Service không phản hồi được sau khi circuit breaker fallback. */
    public static class UpstreamUnavailableException extends RuntimeException {
        public UpstreamUnavailableException(String message) {
            super(message);
        }

        /** Giữ nguyên causal chain (VD JWT_SECRET lệch giữa service) — log/debug không bị mất manh mối. */
        public UpstreamUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
