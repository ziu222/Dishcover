package com.dishcover.rag.client;

import com.dishcover.rag.client.VectorSearchDtos.VectorMatch;
import com.dishcover.rag.client.VectorSearchDtos.VectorSearchRequest;
import com.dishcover.rag.client.VectorSearchDtos.VectorSearchResponse;
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
 * cần Bearer token vì endpoint đó vẫn yêu cầu JWT hợp lệ.
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
     * @param bearerToken token JWT chuyển tiếp (endpoint đích yêu cầu JWT hợp lệ)
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

    /**
     * Giai đoạn B — kênh vector search của {@code HybridRetriever}: gọi
     * {@code POST /matching/internal/vector-search} với embedding câu hỏi đã tính sẵn (RAG tự
     * embed, xem {@code EmbeddingGateway}), lấy top-K {@code recipeId} gần nhất theo cosine.
     *
     * @param bearerToken token JWT chuyển tiếp (endpoint đích yêu cầu JWT hợp lệ)
     * @param embedding   vector câu hỏi đã embed sẵn
     * @param topK        số kết quả tối đa muốn lấy
     * @return danh sách khớp, rỗng nếu Matching Service lỗi/timeout (fail-open — kênh này chỉ là
     *         BỔ SUNG, các kênh Giai đoạn A khác vẫn chạy bình thường)
     */
    @CircuitBreaker(name = "matching-service", fallbackMethod = "fallbackVectorSearch")
    public List<VectorMatch> vectorSearch(String bearerToken, float[] embedding, int topK) {
        VectorSearchResponse result = restClient.post()
                .uri("/matching/internal/vector-search")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(new VectorSearchRequest(embedding, topK))
                .retrieve()
                .body(VectorSearchResponse.class);
        return result == null || result.matches() == null ? List.of() : result.matches();
    }

    @SuppressWarnings("unused")
    private List<VectorMatch> fallbackVectorSearch(String bearerToken, float[] embedding, int topK, Throwable ex) {
        log.warn("Matching Service không phản hồi được (vector search), fallback rỗng: {}", ex.getMessage());
        return List.of();
    }
}
