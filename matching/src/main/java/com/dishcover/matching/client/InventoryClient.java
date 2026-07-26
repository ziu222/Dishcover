package com.dishcover.matching.client;

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
}
