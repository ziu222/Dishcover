package com.dishcover.matching.scoring;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;
import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeIngredientDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Chạy tổ hợp cả 4 rule đúng thứ tự — ví dụ số y hệt bao-cao-thuat-toan-matching.md mục 8
 * (Trứng chiên cà chua, tủ lạnh có trứng gà còn 2 ngày), kết quả mong đợi score ≈ 0.577.
 */
class MatchingEngineTest {

    private final IngredientCatalog catalog = new IngredientCatalog(List.of(
            IngredientEntry.basic("Trứng gà", "trung ga", List.of(), "dam_dong_vat", 21, "trung"),
            IngredientEntry.basic("Cà chua", "ca chua", List.of(), "rau_cu", 7, null),
            IngredientEntry.basic("Hành lá", "hanh la", List.of(), "rau_cu", 5, null)));

    private final MatchingEngine engine = new MatchingEngine(List.of(
            new JaccardBaseRule(),
            new EssentialWeightRule(),
            new ExpiryBonusRule(),
            new AllergyFilterRule(catalog)));

    @Test
    void workedExampleFromReportMatchesExpectedScore() {
        RecipeDetailDto recipe = new RecipeDetailDto("recipe_trung_chien", "Trứng chiên cà chua", "trung-chien-ca-chua",
                null, List.of(
                        new RecipeIngredientDto("trung ga", "trung ga", null, null, true, 1.0),
                        new RecipeIngredientDto("ca chua", "ca chua", null, null, true, 1.0),
                        new RecipeIngredientDto("hanh la", "hanh la", null, null, false, 0.3)), null);

        MatchingContext ctx = new MatchingContext(
                Set.of("trung ga", "sua tuoi", "rau muong"),
                Map.of("trung ga", LocalDate.now().plusDays(2)),
                Set.of(), null);

        double score = engine.score(recipe, ctx);

        // J=0.2, weightedCoverage=(0.5*1.0+0*0.3)/1.3, +bonus 0.5 cho trung ga sắp hết hạn
        double expected = 0.2 * (0.5 / 1.3) + 0.5;
        assertEquals(expected, score, 1e-9);
        assertEquals(0.5769230769230769, score, 1e-9);
    }

    @Test
    void allergyViolationOverridesEverythingElse() {
        RecipeDetailDto recipe = new RecipeDetailDto("id", "n", "s", null,
                List.of(new RecipeIngredientDto("trung ga", "trung ga", null, null, true, 1.0)), null);
        MatchingContext ctx = new MatchingContext(
                Set.of("trung ga"), Map.of("trung ga", LocalDate.now().plusDays(1)), Set.of("trung"), null);

        double score = engine.score(recipe, ctx);
        assertEquals(Double.NEGATIVE_INFINITY, score);
    }
}
