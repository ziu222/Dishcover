package com.dishcover.matching.scoring;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;
import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeIngredientDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllergyFilterRuleTest {

    private final IngredientCatalog catalog = new IngredientCatalog(List.of(
            new IngredientEntry("Trứng gà", "trung ga", List.of(), "dam_dong_vat", 21, "trung"),
            new IngredientEntry("Cà chua", "ca chua", List.of(), "rau_cu", 7, null)));

    private final AllergyFilterRule rule = new AllergyFilterRule(catalog);

    @Test
    void recipeWithoutAllergenIsUnaffected() {
        RecipeDetailDto recipe = new RecipeDetailDto("id", "n", "s", null,
                List.of(new RecipeIngredientDto("ca chua", "ca chua", true, 1.0)));
        MatchingContext ctx = new MatchingContext(Set.of(), Map.of(), Set.of("trung"));
        double score = rule.apply(recipe, ctx, 0.7);
        assertEquals(0.7, score, 1e-9);
    }

    @Test
    void recipeWithAllergenIsHardExcluded() {
        RecipeDetailDto recipe = new RecipeDetailDto("id", "n", "s", null,
                List.of(new RecipeIngredientDto("trung ga", "trung ga", true, 1.0)));
        MatchingContext ctx = new MatchingContext(Set.of(), Map.of(), Set.of("trung"));
        double score = rule.apply(recipe, ctx, 0.9);
        assertEquals(Double.NEGATIVE_INFINITY, score);
    }

    @Test
    void userWithoutMatchingAllergyKeepsScore() {
        RecipeDetailDto recipe = new RecipeDetailDto("id", "n", "s", null,
                List.of(new RecipeIngredientDto("trung ga", "trung ga", true, 1.0)));
        MatchingContext ctx = new MatchingContext(Set.of(), Map.of(), Set.of("hai_san"));
        double score = rule.apply(recipe, ctx, 0.9);
        assertEquals(0.9, score, 1e-9);
    }
}
