package com.dishcover.matching.dto;

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
}
