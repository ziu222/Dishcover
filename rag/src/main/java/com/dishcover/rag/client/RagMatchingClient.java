package com.dishcover.rag.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private static final Logger log = LoggerFactory.getLogger(RagMatchingClient.class);

    private final RestClient restClient;

    /**
     * @param builder {@code RestClient.Builder} riêng cho client nội bộ (timeout 3s/5s, xem
     *                {@link com.dishcover.rag.config.RestClientTimeoutConfig})
     * @param baseUrl địa chỉ Matching Service, cấu hình qua {@code services.matching-url}
     */
    public RagMatchingClient(@Qualifier("internalServiceRestClientBuilder") RestClient.Builder builder,
                              @Value("${services.matching-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Gọi {@code POST /matching/internal/match-by-ingredients} lấy danh sách công thức khớp với
     * nguyên liệu đã trích xuất từ câu hỏi chat.
     *
     * @param bearerToken token JWT chuyển tiếp (endpoint đích vẫn giữ {@code @RequiresPlan("PRO")})
     * @param ingredients nguyên liệu đã normalize (từ {@code IngredientExtractor})
     * @param topN        số lượng công thức tối đa muốn lấy
     * @return danh sách công thức khớp, rỗng nếu Matching Service lỗi/timeout (fail-open, xem
     *         {@code fallbackSearch})
     */
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
        log.warn("Matching Service không phản hồi được, fallback rỗng: {}", ex.getMessage());
        return List.of();
    }
}
