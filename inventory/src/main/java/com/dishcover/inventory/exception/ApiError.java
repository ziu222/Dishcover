package com.dishcover.inventory.exception;

/** Body lỗi thống nhất toàn hệ thống (CLAUDE.md mục 9). */
public record ApiError(String code, String message, String traceId) {
}
