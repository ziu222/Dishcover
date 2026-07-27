package com.dishcover.rag.pipeline;

import java.util.List;

public record RetrievedRecipe(
        String recipeId,
        String name,
        String slug,
        List<String> matchedIngredients,
        List<String> missingIngredients,
        String imageUrl
) {
}
