package com.dishcover.common.ingredient;

import java.util.List;

/**
 * 1 mục trong Ingredient Catalog (CLAUDE.md mục 3.3).
 * normalizedName là khóa so khớp thật giữa Inventory/Recipe/Matching/RAG.
 */
public record IngredientEntry(
        String canonicalName,
        String normalizedName,
        List<String> aliases,
        String category,
        Integer shelfLifeDays,
        String allergenGroup
) {
}
