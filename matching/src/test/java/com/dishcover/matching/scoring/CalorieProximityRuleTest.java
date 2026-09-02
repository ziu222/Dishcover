package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeDetailDto.NutritionDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalorieProximityRuleTest {

    private final CalorieProximityRule rule = new CalorieProximityRule();

    private RecipeDetailDto recipeWithCalories(Double calories) {
        NutritionDto nutrition = calories == null ? null : new NutritionDto(calories);
        return new RecipeDetailDto("id", "n", "s", null, List.of(), nutrition);
    }

    @Test
    void noTargetSetIsNoOp() {
        MatchingContext ctx = new MatchingContext(Set.of(), Map.of(), Set.of(), null);
        double score = rule.apply(recipeWithCalories(500.0), ctx, 1.0);
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void recipeWithoutNutritionIsNoOp() {
        MatchingContext ctx = new MatchingContext(Set.of(), Map.of(), Set.of(), 500);
        double score = rule.apply(recipeWithCalories(null), ctx, 1.0);
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void exactMatchGivesFullBonus() {
        MatchingContext ctx = new MatchingContext(Set.of(), Map.of(), Set.of(), 500);
        double score = rule.apply(recipeWithCalories(500.0), ctx, 1.0);
        assertEquals(2.0, score, 1e-9); // 1.0 + bonus tối đa 1.0
    }

    @Test
    void partialDeviationGivesPartialBonus() {
        // lệch 100/500 = 20% -> bonus = 1 - 0.2 = 0.8
        MatchingContext ctx = new MatchingContext(Set.of(), Map.of(), Set.of(), 500);
        double score = rule.apply(recipeWithCalories(600.0), ctx, 1.0);
        assertEquals(1.8, score, 1e-9);
    }

    @Test
    void deviationAtOrBeyondTargetGivesNoBonus() {
        MatchingContext ctx = new MatchingContext(Set.of(), Map.of(), Set.of(), 500);
        double score = rule.apply(recipeWithCalories(1200.0), ctx, 1.0);
        assertEquals(1.0, score, 1e-9); // lệch vượt 100% -> bonus = 0, không trừ điểm
    }
}
