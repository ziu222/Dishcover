package com.dishcover.matching.exception;

public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 503 — Inventory/Recipe/User Service không phản hồi được sau khi circuit breaker fallback. */
    public static class UpstreamUnavailableException extends RuntimeException {
        public UpstreamUnavailableException(String message) {
            super(message);
        }
    }
}
