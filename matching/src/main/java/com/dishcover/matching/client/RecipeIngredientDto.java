package com.dishcover.matching.client;

/** Map 1 phần tử ingredients[] của RecipeDetailResponse (recipe/dto/RecipeDtos.RecipeIngredientResponse). */
public record RecipeIngredientDto(
        String normalizedName,
        boolean essential,
        double weight
) {
}
