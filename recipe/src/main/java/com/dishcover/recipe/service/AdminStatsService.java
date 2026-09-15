package com.dishcover.recipe.service;

import com.dishcover.recipe.dto.AdminStatsResponse;
import com.dishcover.recipe.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Đếm trực tiếp qua {@code count()} tại thời điểm gọi — không cache, dữ liệu chỉ vài trăm document. */
@Service
public class AdminStatsService {

    private final RecipeRepository recipes;

    public AdminStatsService(RecipeRepository recipes) {
        this.recipes = recipes;
    }

    public AdminStatsResponse stats() {
        Instant sevenDaysAgo = Instant.now().minus(Duration.ofDays(7));
        return new AdminStatsResponse(
                recipes.count(),
                recipes.countByDifficulty("EASY"),
                recipes.countByDifficulty("MEDIUM"),
                recipes.countByDifficulty("HARD"),
                recipes.countByCreatedAtAfter(sevenDaysAgo));
    }
}
