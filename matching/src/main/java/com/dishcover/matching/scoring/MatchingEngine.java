package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;

import java.util.List;

/** Chạy tuần tự chuỗi ScoringRule cho 1 công thức — thêm rule mới = thêm 1 class + đăng ký ở ScoringConfig. */
public class MatchingEngine {

    private final List<ScoringRule> rules;

    public MatchingEngine(List<ScoringRule> rules) {
        this.rules = rules;
    }

    public double score(RecipeDetailDto recipe, MatchingContext ctx) {
        double score = 0.0;
        for (ScoringRule rule : rules) {
            score = rule.apply(recipe, ctx, score);
            if (score == Double.NEGATIVE_INFINITY) {
                break;
            }
        }
        return score;
    }
}
