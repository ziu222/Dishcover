package com.dishcover.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gating tính năng theo gói người dùng (CLAUDE.md mục 8 — bảng freemium). Đặt trên method
 * controller, giá trị là plan tối thiểu yêu cầu (VD "PRO"). Xem {@link RequiresPlanAspect} vì sao
 * hiện chỉ đọc claim JWT thay vì gọi Payment Service thật (Payment Service chưa tồn tại).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequiresPlan {
    String value();
}
