package com.dishcover.recipe.client;

import com.dishcover.recipe.client.IndexDtos.IndexRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Gọi Matching Service {@code POST /internal/index} lúc index công thức (Giai đoạn B, CLAUDE.md
 * mục 6). Fail-open: Matching lỗi/timeout -> chỉ log warn, KHÔNG chặn việc lưu công thức (xem
 * {@code RecipeIndexer}) — vector search chỉ là kênh BỔ SUNG cho chat, thiếu 1 lần index không
 * phải lỗi nghiêm trọng (lần sửa công thức kế tiếp sẽ tự index lại).
 */
@Component
public class MatchingIndexClient {

    private static final Logger log = LoggerFactory.getLogger(MatchingIndexClient.class);

    private final RestClient restClient;

    public MatchingIndexClient(RestClient.Builder builder, @Value("${services.matching-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * @param bearerToken token JWT chuyển tiếp (endpoint đích yêu cầu JWT hợp lệ)
     * @param recipeId    id công thức
     * @param content     văn bản đại diện đã dùng để tính embedding
     * @param embedding   vector đã tính sẵn (RAG Service)
     * @param metadata    thông tin phụ tùy chọn (VD tên món) — chỉ để debug
     */
    @CircuitBreaker(name = "matching-service", fallbackMethod = "fallbackIndex")
    public void index(String bearerToken, String recipeId, String content, float[] embedding,
                       Map<String, Object> metadata) {
        restClient.post()
                .uri("/internal/index")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(new IndexRequest(recipeId, content, embedding, metadata))
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unused")
    private void fallbackIndex(String bearerToken, String recipeId, String content, float[] embedding,
                                Map<String, Object> metadata, Throwable ex) {
        log.warn("Matching Service không phản hồi được lúc index (lưu vector) cho recipeId={}: {}",
                recipeId, ex.getMessage());
    }
}
