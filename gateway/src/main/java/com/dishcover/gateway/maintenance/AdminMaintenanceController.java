package com.dishcover.gateway.maintenance;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

/**
 * Bật/tắt chế độ bảo trì. Sống ngay trong Gateway (không phải service phía sau) vì cờ giữ tại
 * đây (xem {@link MaintenanceState}) — round-trip qua service khác rồi gọi lại Gateway chỉ thêm
 * độ trễ/khả năng lỗi cho một thao tác vốn có thể xử lý tại chỗ.
 *
 * Gateway không có Spring Security (chỉ route thuần), nên tự verify JWT admin ở đây thay vì
 * SecurityConfig — cùng {@link RequestAuth} mà {@link MaintenanceFilter} dùng.
 *
 * Map cả 2 dạng path vì CloudFront (production) forward nguyên "/api/*" tới Gateway còn Vite dev
 * proxy (local) đã cắt "/api" trước khi tới — cùng lý do 7 route "-api-prefixed" trong
 * application.yml.
 */
@RestController
@RequestMapping({"/admin/maintenance", "/api/admin/maintenance"})
public class AdminMaintenanceController {

    private final MaintenanceState state;
    private final RequestAuth requestAuth;

    public AdminMaintenanceController(MaintenanceState state, RequestAuth requestAuth) {
        this.state = state;
        this.requestAuth = requestAuth;
    }

    public record MaintenanceStatus(boolean enabled) {
    }

    @GetMapping
    public ResponseEntity<MaintenanceStatus> status(ServerWebExchange exchange) {
        if (!requestAuth.isAdmin(exchange.getRequest())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(new MaintenanceStatus(state.isEnabled()));
    }

    @PutMapping
    public ResponseEntity<MaintenanceStatus> update(
            ServerWebExchange exchange, @RequestBody MaintenanceStatus body) {
        if (!requestAuth.isAdmin(exchange.getRequest())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        state.set(body.enabled());
        return ResponseEntity.ok(new MaintenanceStatus(state.isEnabled()));
    }
}
