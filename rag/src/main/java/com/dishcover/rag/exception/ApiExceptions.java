package com.dishcover.rag.exception;

public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 503 — Matching/User Service không phản hồi được sau khi circuit breaker fallback. */
    public static class UpstreamUnavailableException extends RuntimeException {
        public UpstreamUnavailableException(String message) {
            super(message);
        }
    }
}
