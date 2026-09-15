package com.dishcover.gateway.maintenance;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cờ bảo trì giữ trong bộ nhớ — không cần DB vì hệ thống chỉ chạy đúng 1 instance Gateway
 * (CLAUDE.md mục 3, không dùng service registry/scale ngang). Đánh đổi chấp nhận được: Gateway
 * restart/redeploy thì cờ tự về false, admin bật lại nếu cần — hợp lý cho một toggle thủ công
 * theo đợt bảo trì, không phải trạng thái nghiệp vụ cần bền vững.
 */
@Component
public class MaintenanceState {

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public boolean isEnabled() {
        return enabled.get();
    }

    public void set(boolean value) {
        enabled.set(value);
    }
}
