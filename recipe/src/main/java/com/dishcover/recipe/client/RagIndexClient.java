package com.dishcover.recipe.client;

import com.dishcover.recipe.client.EmbedDtos.EmbedRequest;
import com.dishcover.recipe.client.EmbedDtos.EmbedResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Gọi RAG Service {@code POST /internal/embed} lúc index công thức (Giai đoạn B, CLAUDE.md mục 6).
 * Fail-open: RAG lỗi/timeout -> {@link Optional#empty()}, KHÔNG chặn việc lưu công thức (xem
 * {@code RecipeIndexer}).
 */
@Component
public class RagIndexClient {

    private static final Logger log = LoggerFactory.getLogger(RagIndexClient.class);

    private final RestClient restClient;

    public RagIndexClient(RestClient.Builder builder, @Value("${services.rag-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * @param bearerToken token JWT chuyển tiếp (endpoint đích yêu cầu JWT hợp lệ)
     * @param text        văn bản đại diện công thức
     * @return vector embedding, rỗng nếu RAG Service không khả dụng
     */
    @CircuitBreaker(name = "rag-service", fallbackMethod = "fallbackEmbed")
    public Optional<float[]> embed(String bearerToken, String text) {
        EmbedResponse res = restClient.post()
                .uri("/internal/embed")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(new EmbedRequest(text))
                .retrieve()
                .body(EmbedResponse.class);
        return res == null ? Optional.empty() : Optional.ofNullable(res.embedding());
    }

    @SuppressWarnings("unused")
    private Optional<float[]> fallbackEmbed(String bearerToken, String text, Throwable ex) {
        log.warn("RAG Service không phản hồi được lúc index (embed), bỏ qua: {}", ex.getMessage());
        return Optional.empty();
    }
}
