package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagPreferenceRuleTest {

    private final TagPreferenceRule rule = new TagPreferenceRule();

    private RecipeDetailDto recipeWithTags(List<String> tags) {
        return new RecipeDetailDto("id", "n", "s", null, List.of(), null, tags);
    }

    private MatchingContext ctx(Set<String> preferredTags) {
        return new MatchingContext(Set.of(), Map.of(), Set.of(), null, preferredTags);
    }

    @Test
    void noPreferredTagsIsNoOp() {
        double score = rule.apply(recipeWithTags(List.of("vegetarian")), ctx(Set.of()), 1.0);
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void recipeWithoutTagsIsNoOp() {
        double score = rule.apply(recipeWithTags(null), ctx(Set.of("vegetarian")), 1.0);
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void oneMatchedTagAddsBonus() {
        double score = rule.apply(recipeWithTags(List.of("vegetarian", "lunch")), ctx(Set.of("vegetarian")), 1.0);
        assertEquals(1.5, score, 1e-9); // 1.0 + BONUS_PER_TAG(0.5) * 1
    }

    @Test
    void multipleMatchedTagsStackBonus() {
        double score = rule.apply(recipeWithTags(List.of("vegetarian", "high protein")),
                ctx(Set.of("vegetarian", "high protein")), 1.0);
        assertEquals(2.0, score, 1e-9); // 1.0 + 0.5*2
    }

    @Test
    void matchIsCaseInsensitive() {
        double score = rule.apply(recipeWithTags(List.of("High Protein")), ctx(Set.of("high protein")), 1.0);
        assertEquals(1.5, score, 1e-9);
    }

    @Test
    void unmatchedTagsGiveNoBonus() {
        double score = rule.apply(recipeWithTags(List.of("dessert")), ctx(Set.of("vegetarian")), 1.0);
        assertEquals(1.0, score, 1e-9);
    }

    /**
     * Phát hiện lúc live-verify thật (2026-09-07): vocabulary tag thật của Spoonacular dùng
     * "lacto ovo vegetarian"/"paleolithic" chứ không phải "vegetarian"/"paleo" — nếu chỉ so khớp
     * bằng tuyệt đối, người dùng gõ đúng từ spec liệt kê ("vegetarian") sẽ KHÔNG BAO GIỜ khớp được
     * bất kỳ công thức nào. So khớp phải theo kiểu bao hàm chuỗi con (2 chiều).
     */
    @Test
    void substringMatchCatchesCompoundRealWorldTags() {
        double score = rule.apply(recipeWithTags(List.of("lacto ovo vegetarian")), ctx(Set.of("vegetarian")), 1.0);
        assertEquals(1.5, score, 1e-9);
    }

    @Test
    void substringMatchWorksInReverseDirectionToo() {
        double score = rule.apply(recipeWithTags(List.of("paleolithic")), ctx(Set.of("paleo")), 1.0);
        assertEquals(1.5, score, 1e-9);
    }
}
