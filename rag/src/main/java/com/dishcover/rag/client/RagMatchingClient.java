package com.dishcover.rag.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Gọi Matching Service /internal/match-by-ingredients (specs/rag-service.md mục 1.1/3.3) — vẫn
 * cần Bearer token vì endpoint đó giữ @RequiresPlan("PRO") (mục 1.2).
 */
@Component
public class RagMatchingClient {

    private final RestClient restClient;

    public RagMatchingClient(RestClient.Builder builder,
                              @Value("${services.matching-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "matching-service", fallbackMethod = "fallbackSearch")
    public List<RecipeMatchDto> searchByIngredients(String bearerToken, List<String> ingredients, int topN) {
        List<RecipeMatchDto> result = restClient.post()
                .uri("/matching/internal/match-by-ingredients")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(new MatchByIngredientsRequestDto(ingredients, topN))
                .retrieve()
                .body(new ParameterizedTypeReference<List<RecipeMatchDto>>() {
                });
        return result != null ? result : List.of();
    }

    /** Matching down -> "không tìm thấy công thức" là trạng thái degrade AN TOÀN cho chat (fail-open). */
    @SuppressWarnings("unused")
    private List<RecipeMatchDto> fallbackSearch(String bearerToken, List<String> ingredients, int topN, Throwable ex) {
        return List.of();
    }
}
