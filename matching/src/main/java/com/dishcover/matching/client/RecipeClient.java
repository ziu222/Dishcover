package com.dishcover.matching.client;

import com.dishcover.common.exception.ResourceNotFoundException;
import com.dishcover.matching.exception.ApiExceptions.UpstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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

    /**
     * @param builder builder RestClient dùng chung của service (timeout áp qua {@code RestClientCustomizer})
     * @param baseUrl địa chỉ gốc của Recipe Service, nạp từ {@code services.recipe-url}
     */
    public RecipeClient(RestClient.Builder builder,
                         @Value("${services.recipe-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Lấy toàn bộ công thức kèm ingredients — gọi GET /recipes (public, không cần token) lấy danh
     * sách id rồi gọi thêm GET /recipes/{id} cho từng công thức (N+1 có chủ đích, chấp nhận ở quy
     * mô hiện tại). Khi Recipe Service lỗi/timeout, circuit breaker gọi fallback NÉM
     * {@link com.dishcover.matching.exception.ApiExceptions.UpstreamUnavailableException}
     * (fail-closed) thay vì trả danh sách rỗng, vì không có gì để chấm điểm thì không thể giả vờ
     * kết quả "0 công thức phù hợp".
     *
     * @return danh sách toàn bộ công thức kèm nguyên liệu
     * @throws com.dishcover.matching.exception.ApiExceptions.UpstreamUnavailableException nếu Recipe
     *         Service không khả dụng
     */
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

    /**
     * Lấy chi tiết 1 công thức theo id — dùng cho endpoint availability (so số lượng đủ/thiếu nguyên
     * liệu), khác {@link #getAllRecipesWithIngredients} vốn lấy toàn bộ để chấm điểm gợi ý.
     *
     * @param id id công thức bên Recipe Service
     * @return chi tiết công thức
     * @throws ResourceNotFoundException nếu không tìm thấy công thức với id chỉ định
     * @throws UpstreamUnavailableException nếu Recipe Service không khả dụng (khác 404)
     */
    @CircuitBreaker(name = "recipe-service", fallbackMethod = "fallbackGetRecipeById")
    public RecipeDetailDto getRecipeById(String id) {
        try {
            return restClient.get().uri("/recipes/{id}", id).retrieve().body(RecipeDetailDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Không tìm thấy công thức id=" + id);
        }
    }

    /** 404 không phải "Recipe Service down" — cho đi qua nguyên trạng, chỉ fallback lỗi khác thành 503. */
    @SuppressWarnings("unused")
    private RecipeDetailDto fallbackGetRecipeById(String id, Throwable ex) {
        if (ex instanceof ResourceNotFoundException notFound) {
            throw notFound;
        }
        throw new UpstreamUnavailableException("Recipe Service tạm thời không khả dụng, thử lại sau");
    }
}
