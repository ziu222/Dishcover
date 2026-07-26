package com.dishcover.recipe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public final class RecipeDtos {

    private RecipeDtos() {
    }

    /** Client chỉ gửi essential (true/false) — server tự tính weight (1.0/0.3), tự tính normalizedName qua catalog. */
    public record RecipeIngredientRequest(
            @NotBlank String name,
            Double amount,
            String unit,
            boolean essential
    ) {
    }

    public record RecipeStepRequest(
            @NotNull @Min(1) Integer order,
            @NotBlank String title,
            @NotBlank String content,
            Integer durationMinutes
    ) {
    }

    public record CreateRecipeRequest(
            @NotBlank String name,
            String slug, // optional — server tự sinh từ name nếu không gửi
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

    public record RecipeIngredientResponse(
            String name, String normalizedName, Double amount, String unit, boolean essential, double weight
    ) {
    }

    public record RecipeStepResponse(int order, String title, String content, int durationMinutes) {
    }

    /** Dùng cho list — không kèm ingredients/steps để payload nhẹ. */
    public record RecipeSummaryResponse(
            String id, String name, String slug, int cookTimeMinutes,
            String difficulty, List<String> tags, String imageUrl
    ) {
    }

    public record RecipeDetailResponse(
            String id, String name, String slug, int cookTimeMinutes, String difficulty,
            List<String> tags, List<String> dietaryFlags,
            List<RecipeIngredientResponse> ingredients, List<RecipeStepResponse> steps,
            String imageUrl, String videoUrl
    ) {
    }
}
