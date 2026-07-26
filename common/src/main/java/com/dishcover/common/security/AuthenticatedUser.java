package com.dishcover.common.security;

/** Principal đặt vào SecurityContext sau khi JWT được verify thành công. */
public record AuthenticatedUser(Long userId, String email, String plan) {
}
