package com.dishcover.gateway.maintenance;

import com.dishcover.common.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bảo trì là logic chặn có nhánh (admin bypass, login exempt, bật/tắt) — đúng loại cần 1 bài test
 * chạy được thay vì chỉ tin đọc code, theo CLAUDE.md mục 9 "Test tối thiểu ưu tiên".
 */
class MaintenanceFilterTest {

    private static final String SECRET = "a".repeat(32);

    private MaintenanceState state;
    private MaintenanceFilter filter;
    private AtomicBoolean chainCalled;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService(SECRET, 60);
        state = new MaintenanceState();
        filter = new MaintenanceFilter(state, new RequestAuth(jwtService));
        chainCalled = new AtomicBoolean(false);
    }

    private Mono<Void> run(MockServerHttpRequest request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        return filter.filter(exchange, ex -> {
            chainCalled.set(true);
            return Mono.empty();
        }).doOnSuccess(v -> {
            if (!chainCalled.get()) {
                assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            }
        });
    }

    @Test
    void choKhongChanKhiBaoTriTat() {
        state.set(false);
        run(MockServerHttpRequest.get("/recipe-service/recipes").build()).block();
        assertThat(chainCalled).isTrue();
    }

    @Test
    void chanRequestThuongKhiBaoTriBat() {
        state.set(true);
        run(MockServerHttpRequest.get("/recipe-service/recipes").build()).block();
        assertThat(chainCalled).isFalse();
    }

    @Test
    void choDangNhapDiQuaKeCaKhiBaoTriBat() {
        state.set(true);
        run(MockServerHttpRequest.method(HttpMethod.POST, "/user-service/auth/login").build()).block();
        assertThat(chainCalled).isTrue();
    }

    @Test
    void choAdminDiQuaKeCaKhiBaoTriBat() {
        state.set(true);
        String adminToken = new JwtService(SECRET, 60).issue(1L, "admin@larder.vn", "FREE", "ADMIN");
        run(MockServerHttpRequest.get("/recipe-service/recipes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .build()).block();
        assertThat(chainCalled).isTrue();
    }

    @Test
    void chanUserThuongKeCaCoTokenHopLe() {
        state.set(true);
        String userToken = new JwtService(SECRET, 60).issue(1L, "user@larder.vn", "FREE", "USER");
        run(MockServerHttpRequest.get("/recipe-service/recipes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .build()).block();
        assertThat(chainCalled).isFalse();
    }
}
