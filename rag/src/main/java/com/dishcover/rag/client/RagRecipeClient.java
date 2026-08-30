package com.dishcover.rag.client;

import com.dishcover.common.text.VietnameseTextNormalizer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Kênh truy hồi THỨ 2 của {@link com.dishcover.rag.pipeline.HybridRetriever}: tìm công thức theo
 * TÊN MÓN thay vì nguyên liệu — bù cho {@code IngredientExtractor} chỉ trích được nguyên liệu, nên
 * câu hỏi kiểu "Cho tôi công thức Phở bò" trước đây luôn rỗng dù món tồn tại thật (phát hiện qua
 * bộ eval, xem eval/results/chatbot-report.md).
 *
 * <p>Cách khớp: lấy toàn bộ tên công thức (GET /recipes, public, không cần token — chỉ có ~131
 * món nên 1 lần gọi rẻ hơn N+1 fetch của Matching Service), rồi kiểm tra tên món (đã normalize)
 * có xuất hiện làm chuỗi con trong câu hỏi (đã normalize) hay không — chiều ngược lại so với
 * {@code findByNormalizedNameContaining} bên Recipe Service (query đó tìm theo 1 từ khóa NGẮN,
 * không hợp để truyền cả câu hỏi tự nhiên dài vào).</p>
 *
 * <p>ponytail: so khớp bằng {@code contains()} đơn giản, không giới hạn độ dài tên tối thiểu —
 * rủi ro tên món 1 từ khớp nhầm, nhưng 131 tên hiện tại đều ≥2 từ nên chưa xảy ra; nếu sau này có
 * tên món 1 từ trùng từ thông dụng thì thêm ngưỡng độ dài.</p>
 */
@Component
public class RagRecipeClient {

    private static final Logger log = LoggerFactory.getLogger(RagRecipeClient.class);
    private static final int MAX_NAME_MATCHES = 3;

    private final RestClient restClient;

    /**
     * @param builder {@code RestClient.Builder} riêng cho client nội bộ (timeout 3s/5s)
     * @param baseUrl địa chỉ Recipe Service, cấu hình qua {@code services.recipe-url}
     */
    public RagRecipeClient(@Qualifier("internalServiceRestClientBuilder") RestClient.Builder builder,
                            @Value("${services.recipe-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Tìm công thức mà TÊN xuất hiện trong câu hỏi, kèm chi tiết nguyên liệu (để dựng prompt và
     * để {@code HybridRetriever} lọc dị ứng đúng như kênh nguyên liệu).
     *
     * @param question câu hỏi gốc của người dùng
     * @return tối đa {@link #MAX_NAME_MATCHES} công thức khớp tên, rỗng nếu không khớp/Recipe
     *         Service lỗi (fail-open — giống {@code RagMatchingClient}, thiếu kênh này vẫn còn
     *         kênh nguyên liệu)
     */
    @CircuitBreaker(name = "recipe-service", fallbackMethod = "fallbackSearchByName")
    public List<RecipeDetailDto> searchByName(String question) {
        String normalizedQuestion = VietnameseTextNormalizer.normalize(question);
        PageDto<RecipeSummaryDto> page = restClient.get()
                .uri("/recipes?size=500")
                .retrieve()
                .body(new ParameterizedTypeReference<PageDto<RecipeSummaryDto>>() {
                });
        if (page == null || page.content() == null) {
            return List.of();
        }
        return page.content().stream()
                .filter(r -> normalizedQuestion.contains(VietnameseTextNormalizer.normalize(r.name())))
                .limit(MAX_NAME_MATCHES)
                .map(r -> restClient.get()
                        .uri("/recipes/{id}", r.id())
                        .retrieve()
                        .body(RecipeDetailDto.class))
                .toList();
    }

    /** Recipe Service down -> mất kênh tên món, kênh nguyên liệu vẫn chạy bình thường (fail-open). */
    @SuppressWarnings("unused")
    private List<RecipeDetailDto> fallbackSearchByName(String question, Throwable ex) {
        log.warn("Recipe Service không phản hồi được (kênh tên món), fallback rỗng: {}", ex.getMessage());
        return List.of();
    }
}
