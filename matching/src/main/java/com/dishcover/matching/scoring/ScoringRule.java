package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;

/** Strategy chấm điểm 1 công thức — CLAUDE.md mục 5, chuỗi Open/Closed (thêm rule mới không sửa rule cũ). */
public interface ScoringRule {
    double apply(RecipeDetailDto recipe, MatchingContext ctx, double currentScore);
}
