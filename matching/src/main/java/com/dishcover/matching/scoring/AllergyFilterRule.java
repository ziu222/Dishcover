package com.dishcover.matching.scoring;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;
import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeIngredientDto;

/**
 * Loại cứng công thức chứa nguyên liệu thuộc nhóm dị ứng người dùng khai báo — ràng buộc an toàn,
 * không phải điểm số. bao-cao-thuat-toan-matching.md mục 6, specs/matching-service.md mục 3.2 (4).
 */
public class AllergyFilterRule implements ScoringRule {

    private final IngredientCatalog catalog;

    public AllergyFilterRule(IngredientCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public double apply(RecipeDetailDto recipe, MatchingContext ctx, double currentScore) {
        boolean violatesAllergy = recipe.ingredients().stream().anyMatch(i -> violates(i, ctx));
        return violatesAllergy ? Double.NEGATIVE_INFINITY : currentScore;
    }

    private boolean violates(RecipeIngredientDto ingredient, MatchingContext ctx) {
        return catalog.lookup(ingredient.normalizedName())
                .map(IngredientEntry::allergenGroup)
                .filter(group -> group != null && ctx.userAllergenGroups().contains(group))
                .isPresent();
    }
}
