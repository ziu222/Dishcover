package com.dishcover.matching.client;

import com.dishcover.matching.exception.ApiExceptions.UpstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Gọi Recipe Service (public, không cần token) để lấy toàn bộ công thức kèm ingredients.
 * GET /recipes chỉ trả summary -> phải gọi thêm GET /recipes/{id} cho từng cái (N+1 có chủ đích,
 * chấp nhận ở quy mô hiện tại — specs/matching-service.md mục 3.1).
 */
@Component
public class RecipeClient {

    private static final int PAGE_SIZE = 500;

    private final RestClient restClient;

    public RecipeClient(RestClient.Builder builder,
                         @Value("${services.recipe-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "recipe-service", fallbackMethod = "fallbackGetAllRecipes")
    public List<RecipeDetailDto> getAllRecipesWithIngredients() {
        PageDto<RecipeSummaryDto> page = restClient.get()
                .uri("/recipes?size={size}", PAGE_SIZE)
                .retrieve()
                .body(new ParameterizedTypeReference<PageDto<RecipeSummaryDto>>() {
                });
        if (page == null || page.content() == null) {
            return List.of();
        }
        return page.content().stream()
                .map(summary -> restClient.get()
                        .uri("/recipes/{id}", summary.id())
                        .retrieve()
                        .body(RecipeDetailDto.class))
                .toList();
    }

    /** Recipe down -> không có gì để chấm điểm, không thể giả vờ trả danh sách rỗng như gợi ý "0 kết quả". */
    @SuppressWarnings("unused")
    private List<RecipeDetailDto> fallbackGetAllRecipes(Throwable ex) {
        throw new UpstreamUnavailableException("Recipe Service tạm thời không khả dụng, thử lại sau");
    }
}
