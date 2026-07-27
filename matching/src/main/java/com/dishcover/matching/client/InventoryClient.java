package com.dishcover.matching.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Gọi Inventory Service để lấy nguyên liệu người dùng đang có (specs/matching-service.md mục 3.1). */
@Component
public class InventoryClient {

    private static final Set<String> NOT_AVAILABLE_STATUS = Set.of("USED", "EXPIRED");

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder,
                            @Value("${services.inventory-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /** Chỉ trả nguyên liệu còn "đang có" — loại USED (đã dùng) và EXPIRED (đã hỏng) khỏi tập chấm điểm. */
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackGetFreshItems")
    public List<InventoryItemDto> getFreshItems(String bearerToken) {
        InventoryItemDto[] items = restClient.get()
                .uri("/inventory/items")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(InventoryItemDto[].class);
        if (items == null) {
            return List.of();
        }
        return Arrays.stream(items)
                .filter(i -> !NOT_AVAILABLE_STATUS.contains(i.status()))
                .toList();
    }

    /**
     * Inventory down -> coi như "chưa có nguyên liệu nào" thay vì crash toàn bộ gợi ý — an toàn vì
     * đây chỉ làm điểm số thấp/gợi ý kém đi, KHÔNG như UserClient (dị ứng) nơi giả định sai có thể
     * gây hại (specs/matching-service.md mục 3.5 review).
     */
    @SuppressWarnings("unused")
    private List<InventoryItemDto> fallbackGetFreshItems(String bearerToken, Throwable ex) {
        return List.of();
    }
}
