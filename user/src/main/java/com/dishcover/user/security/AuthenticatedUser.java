package com.dishcover.user.security;

/** Principal đặt vào SecurityContext sau khi verify JWT. */
public record AuthenticatedUser(Long userId, String email, String plan) {
}
