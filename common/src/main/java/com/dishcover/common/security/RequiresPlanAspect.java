package com.dishcover.common.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Lớp gating 2 (CLAUDE.md mục 8) — kiểm tra {@link AuthenticatedUser#plan()} lấy từ
 * SecurityContext (đã verify JWT ở JwtAuthFilter trước đó trong filter chain).
 *
 * <p><b>Cố ý đơn giản hóa</b>: chỉ đọc claim {@code plan} có sẵn trong JWT, KHÔNG gọi Payment
 * Service thật (chưa tồn tại — checklist mục 10). Khi Payment Service xong, chỉ cần sửa bên
 * trong phương thức {@link #checkPlan} để gọi {@code PaymentClient.isActive()} (cache 5 phút) —
 * không đụng bất kỳ nơi nào dùng {@code @RequiresPlan}. Đánh đổi: claim {@code plan} có thể "cũ"
 * tới khi user đăng nhập lại.</p>
 */
@Aspect
@Component
public class RequiresPlanAspect {

    @Around("@annotation(requiresPlan)")
    public Object checkPlan(ProceedingJoinPoint joinPoint, RequiresPlan requiresPlan) throws Throwable {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AuthenticatedUser user && requiresPlan.value().equals(user.plan())) {
            return joinPoint.proceed();
        }
        throw new PlanRequiredException(requiresPlan.value());
    }
}
