package com.dishcover.recipe.dto;

/** Số liệu tổng quan công thức cho màn /admin/so-lieu. */
public record AdminStatsResponse(
        long totalRecipes,
        long easyCount,
        long mediumCount,
        long hardCount,
        long newRecipesLast7Days
) {
}
