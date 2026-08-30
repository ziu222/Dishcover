package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeIngredientDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JaccardBaseRuleTest {

    private final JaccardBaseRule rule = new JaccardBaseRule();

    private RecipeDetailDto recipe(String... normalizedNames) {
        List<RecipeIngredientDto> ingredients = List.of(normalizedNames).stream()
                .map(n -> new RecipeIngredientDto(n, n, true, 1.0))
                .toList();
        return new RecipeDetailDto("id", "name", "slug", null, ingredients);
    }

    private MatchingContext ctx(String... userNormalizedNames) {
        return new MatchingContext(Set.of(userNormalizedNames), Map.of(), Set.of());
    }

    @Test
    void noOverlapGivesZero() {
        double score = rule.apply(recipe("trung ga", "ca chua"), ctx("sua tuoi"), 0);
        assertEquals(0.0, score, 1e-9);
    }

    @Test
    void fullOverlapGivesOne() {
        double score = rule.apply(recipe("trung ga", "ca chua"), ctx("trung ga", "ca chua"), 0);
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void partialOverlapMatchesFormula() {
        // R={trung ga, ca chua, hanh la}, U={trung ga, sua tuoi, rau muong}
        // R∩U={trung ga}=1, R∪U={trung ga,ca chua,hanh la,sua tuoi,rau muong}=5 -> 1/5=0.2
        double score = rule.apply(recipe("trung ga", "ca chua", "hanh la"),
                ctx("trung ga", "sua tuoi", "rau muong"), 0);
        assertEquals(0.2, score, 1e-9);
    }

    @Test
    void ignoresCurrentScoreBeingFirstRule() {
        double score = rule.apply(recipe("trung ga"), ctx("trung ga"), 999);
        assertEquals(1.0, score, 1e-9);
    }
}
