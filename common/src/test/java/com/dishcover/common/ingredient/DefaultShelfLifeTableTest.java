package com.dishcover.common.ingredient;

import org.junit.jupiter.api.Test;

import static com.dishcover.common.ingredient.DefaultShelfLifeTable.GLOBAL_FALLBACK_DAYS;
import static com.dishcover.common.ingredient.DefaultShelfLifeTable.forCategory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultShelfLifeTableTest {

    @Test
    void returnsCategoryDefaults() {
        assertEquals(5, forCategory("la_thom"));
        assertEquals(2, forCategory("hai_san"));
        assertEquals(180, forCategory("do_kho"));
    }

    @Test
    void unknownCategoryFallsBackToGlobal() {
        assertEquals(GLOBAL_FALLBACK_DAYS, forCategory("category_la"));
    }

    @Test
    void nullCategoryFallsBackToGlobal() {
        assertEquals(GLOBAL_FALLBACK_DAYS, forCategory(null));
    }

    @Test
    void knownCategoryDefaults() {
        assertEquals(10, forCategory("rau_cu"));
        assertEquals(3, forCategory("thit"));
        assertEquals(14, forCategory("trung_sua"));
    }

    @Test
    void everyCatalogCategoryHasAnExplicitDefault() {
        // guard: nếu ai thêm category mới vào catalog mà quên khai default → test fail
        IngredientCatalog catalog = IngredientCatalog.loadDefault();
        catalog.entries().stream()
                .map(IngredientEntry::category)
                .distinct()
                .forEach(cat -> assertTrue(
                        DefaultShelfLifeTable.knownCategories().contains(cat),
                        "category '" + cat + "' trong catalog nhưng thiếu default trong DefaultShelfLifeTable"));
    }
}
