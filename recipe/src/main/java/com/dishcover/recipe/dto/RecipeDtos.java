package com.dishcover.recipe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Tập hợp record DTO dùng cho request/response của Recipe Service. Tách khỏi entity JPA/Mongo
 * theo nguyên tắc DTO ≠ Entity (CLAUDE.md mục 9) — API không bao giờ expose entity trực tiếp.
 */
public final class RecipeDtos {

    private RecipeDtos() {
    }

    /**
     * Client chỉ gửi essential (true/false) — server tự tính weight (1.0/0.3), tự tính
     * normalizedName qua catalog.
     *
     * @param name       tên nguyên liệu do client nhập
     * @param essential  true nếu là nguyên liệu chính, false nếu là nguyên liệu phụ
     */
    public record RecipeIngredientRequest(
            @NotBlank String name,
            Double amount,
            String unit,
            boolean essential
    ) {
    }

    /**
     * Payload mô tả một bước nấu trong request tạo/sửa công thức.
     */
    public record RecipeStepRequest(
            @NotNull @Min(1) Integer order,
            @NotBlank String title,
            @NotBlank String content,
            Integer durationMinutes
    ) {
    }

    /**
     * Payload tạo mới công thức (dùng cho {@code POST /recipes}).
     *
     * @param slug  tùy chọn — server tự sinh từ name nếu không gửi
     */
    public record CreateRecipeRequest(
            @NotBlank String name,
            String slug,
            @Min(0) int cookTimeMinutes,
            @Pattern(regexp = "EASY|MEDIUM|HARD", message = "difficulty phải là EASY, MEDIUM hoặc HARD") String difficulty,
            List<String> tags,
            List<String> dietaryFlags,
            @NotEmpty @Valid List<RecipeIngredientRequest> ingredients,
            @NotEmpty @Valid List<RecipeStepRequest> steps,
            String imageUrl,
            String videoUrl
    ) {
    }

    /** Mọi field optional — PATCH chỉ áp field nào client thực sự gửi. */
    public record UpdateRecipeRequest(
            String name,
            Integer cookTimeMinutes,
            @Pattern(regexp = "EASY|MEDIUM|HARD", message = "difficulty phải là EASY, MEDIUM hoặc HARD") String difficulty,
            List<String> tags,
            List<String> dietaryFlags,
            @Valid List<RecipeIngredientRequest> ingredients,
            @Valid List<RecipeStepRequest> steps,
            String imageUrl,
            String videoUrl
    ) {
    }

    /**
     * Thông tin một nguyên liệu trả về trong response chi tiết công thức.
     *
     * @param normalizedName  tên nguyên liệu đã chuẩn hóa (bỏ dấu, lowercase)
     * @param essential       true nếu là nguyên liệu chính, false nếu là nguyên liệu phụ
     * @param weight          trọng số dùng cho thuật toán chấm điểm matching
     */
    public record RecipeIngredientResponse(
            String name, String normalizedName, Double amount, String unit, boolean essential, double weight
    ) {
    }

    /**
     * Thông tin một bước nấu trả về trong response chi tiết công thức.
     */
    public record RecipeStepResponse(int order, String title, String content, int durationMinutes) {
    }

    /** Dùng cho list — không kèm ingredients/steps để payload nhẹ. */
    public record RecipeSummaryResponse(
            String id, String name, String slug, int cookTimeMinutes,
            String difficulty, List<String> tags, String imageUrl
    ) {
    }

    /**
     * Response đầy đủ của một công thức, dùng cho {@code GET /recipes/{id}} và sau khi
     * create/update.
     */
    public record RecipeDetailResponse(
            String id, String name, String slug, int cookTimeMinutes, String difficulty,
            List<String> tags, List<String> dietaryFlags,
            List<RecipeIngredientResponse> ingredients, List<RecipeStepResponse> steps,
            String imageUrl, String videoUrl
    ) {
    }
}
