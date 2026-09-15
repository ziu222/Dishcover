package com.dishcover.gateway.maintenance;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Chặn mọi request khi hệ thống đang bảo trì, TRỪ:
 *  - đăng nhập (để admin còn vào lại được sau khi đã bật bảo trì)
 *  - request mang JWT hợp lệ role ADMIN (admin dùng app bình thường kể cả khi bảo trì)
 *  - health check của ALB (/actuator/health) — chặn nhầm sẽ khiến ECS coi Gateway "unhealthy"
 *    và tự khởi động lại vòng lặp, không phải lỗi thật.
 *
 * Đặt ngay tại Gateway vì đây là điểm duy nhất expose port ra ngoài (CLAUDE.md mục 3) — không cần
 * lặp lại logic này ở 7 service phía sau.
 */
@Component
public class MaintenanceFilter implements GlobalFilter, Ordered {

    private static final List<String> LOGIN_PATHS =
            List.of("/user-service/auth/login", "/api/user-service/auth/login");

    private final MaintenanceState state;
    private final RequestAuth requestAuth;

    public MaintenanceFilter(MaintenanceState state, RequestAuth requestAuth) {
        this.state = state;
        this.requestAuth = requestAuth;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!state.isEnabled() || isExempt(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        return reject(exchange);
    }

    private boolean isExempt(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        if (path.startsWith("/actuator/")) return true;
        if (request.getMethod() == HttpMethod.POST && LOGIN_PATHS.contains(path)) return true;
        return requestAuth.isAdmin(request);
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"code\":\"MAINTENANCE_MODE\",\"message\":"
                + "\"Hệ thống đang bảo trì, vui lòng quay lại sau.\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
