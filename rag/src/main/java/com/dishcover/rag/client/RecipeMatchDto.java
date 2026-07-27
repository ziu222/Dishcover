package com.dishcover.rag.client;

import java.util.List;

/** Mirror của matching/dto/MatchingDtos.RecipeMatchResponse — response của Matching Service. */
public record RecipeMatchDto(
        String recipeId,
        String name,
        String slug,
        double score,
        List<String> matchedIngredients,
        List<String> missingIngredients,
        String imageUrl
) {
}
