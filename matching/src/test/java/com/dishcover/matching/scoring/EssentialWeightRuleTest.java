package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeIngredientDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EssentialWeightRuleTest {

    private final EssentialWeightRule rule = new EssentialWeightRule();

    private MatchingContext ctx(String... userNormalizedNames) {
        return new MatchingContext(Set.of(userNormalizedNames), Map.of(), Set.of(), null);
    }

    @Test
    void allEssentialMatchedGivesFullWeight() {
        RecipeDetailDto recipe = new RecipeDetailDto("id", "n", "s", null, List.of(
                new RecipeIngredientDto("trung ga", "trung ga", null, null, true, 1.0),
                new RecipeIngredientDto("ca chua", "ca chua", null, null, true, 1.0)), null);
        double score = rule.apply(recipe, ctx("trung ga", "ca chua"), 1.0);
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void noOptionalIngredientsDefaultsCoverageToOne() {
        // recipe chỉ có essential, không có nguyên liệu phụ -> optionalCoverage quy ước = 1.0
        RecipeDetailDto recipe = new RecipeDetailDto("id", "n", "s", null, List.of(
                new RecipeIngredientDto("trung ga", "trung ga", null, null, true, 1.0)), null);
        double score = rule.apply(recipe, ctx("trung ga"), 1.0);
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void noEssentialIngredientsDefaultsCoverageToOne() {
        // recipe chỉ có nguyên liệu phụ, không có essential -> essentialCoverage quy ước = 1.0
        RecipeDetailDto recipe = new RecipeDetailDto("id", "n", "s", null, List.of(
                new RecipeIngredientDto("hanh la", "hanh la", null, null, false, 0.3)), null);
        double score = rule.apply(recipe, ctx(), 1.0);
        // essentialCoverage=1.0 (rỗng), optionalCoverage=0/1=0 -> (1*1.0 + 0*0.3)/1.3
        assertEquals(1.0 / 1.3, score, 1e-9);
    }

    @Test
    void missingEssentialPenalizesMoreThanMissingOptional() {
        // trứng chiên cà chua: essential={trung ga, ca chua}, phụ={hanh la}; user chỉ có trung ga
        RecipeDetailDto recipe = new RecipeDetailDto("id", "n", "s", null, List.of(
                new RecipeIngredientDto("trung ga", "trung ga", null, null, true, 1.0),
                new RecipeIngredientDto("ca chua", "ca chua", null, null, true, 1.0),
                new RecipeIngredientDto("hanh la", "hanh la", null, null, false, 0.3)), null);
        double score = rule.apply(recipe, ctx("trung ga"), 1.0);
        // essentialCoverage=1/2=0.5, optionalCoverage=0/1=0 -> (0.5*1.0 + 0*0.3)/1.3
        assertEquals(0.5 / 1.3, score, 1e-9);
    }
}
