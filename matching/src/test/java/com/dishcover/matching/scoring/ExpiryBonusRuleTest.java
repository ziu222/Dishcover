package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeIngredientDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpiryBonusRuleTest {

    private final ExpiryBonusRule rule = new ExpiryBonusRule();

    private RecipeDetailDto recipeWith(String... normalizedNames) {
        List<RecipeIngredientDto> ingredients = List.of(normalizedNames).stream()
                .map(n -> new RecipeIngredientDto(n, true, 1.0))
                .toList();
        return new RecipeDetailDto("id", "n", "s", null, ingredients);
    }

    @Test
    void noMatchedIngredientsGivesNoBonus() {
        MatchingContext ctx = new MatchingContext(Set.of(), Map.of(), Set.of());
        double score = rule.apply(recipeWith("trung ga"), ctx, 1.0);
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void matchedIngredientFarFromExpiryGivesNoBonus() {
        MatchingContext ctx = new MatchingContext(
                Set.of("trung ga"),
                Map.of("trung ga", LocalDate.now().plusDays(10)),
                Set.of());
        double score = rule.apply(recipeWith("trung ga"), ctx, 1.0);
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void matchedIngredientWithinThresholdAddsBonus() {
        MatchingContext ctx = new MatchingContext(
                Set.of("trung ga"),
                Map.of("trung ga", LocalDate.now().plusDays(2)),
                Set.of());
        double score = rule.apply(recipeWith("trung ga"), ctx, 1.0);
        assertEquals(1.5, score, 1e-9); // 1.0 + BONUS_PER_ITEM(0.5) * 1
    }

    @Test
    void multipleExpiringIngredientsStackBonus() {
        MatchingContext ctx = new MatchingContext(
                Set.of("trung ga", "ca chua"),
                Map.of("trung ga", LocalDate.now().plusDays(1), "ca chua", LocalDate.now().plusDays(3)),
                Set.of());
        double score = rule.apply(recipeWith("trung ga", "ca chua"), ctx, 0.0);
        assertEquals(1.0, score, 1e-9); // 0.0 + 0.5*2
    }
}
