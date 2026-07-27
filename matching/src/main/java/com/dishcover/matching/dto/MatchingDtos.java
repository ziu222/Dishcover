package com.dishcover.matching.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public final class MatchingDtos {

    private MatchingDtos() {
    }

    public record RecipeMatchResponse(
            String recipeId,
            String name,
            String slug,
            double score,
            List<String> matchedIngredients,
            List<String> missingIngredients,
            String imageUrl
    ) {
    }

    /** Dùng cho POST /internal/match-by-ingredients — RAG gọi với nguyên liệu trích từ câu hỏi chat. */
    public record MatchByIngredientsRequest(
            @NotEmpty List<String> ingredients,
            Integer topN
    ) {
    }
}
