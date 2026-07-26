package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeIngredientDto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Điểm nền — Jaccard thô trên tập normalizedName: |R∩U| / |R∪U|.
 * bao-cao-thuat-toan-matching.md mục 3, specs/matching-service.md mục 3.2 (1).
 */
public class JaccardBaseRule implements ScoringRule {

    @Override
    public double apply(RecipeDetailDto recipe, MatchingContext ctx, double currentScore) {
        Set<String> r = recipe.ingredients().stream()
                .map(RecipeIngredientDto::normalizedName)
                .collect(Collectors.toSet());
        if (r.isEmpty()) {
            return 0.0;
        }
        Set<String> u = ctx.userNormalizedNames();

        Set<String> intersection = new HashSet<>(r);
        intersection.retainAll(u);

        Set<String> union = new HashSet<>(r);
        union.addAll(u);

        return (double) intersection.size() / union.size();
    }
}
