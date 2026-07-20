package com.dishcover.common.ingredient;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IngredientCatalogTest {

    private final IngredientCatalog catalog = IngredientCatalog.loadDefault();

    @Test
    void loadsNonEmptyCatalog() {
        assertTrue(catalog.size() >= 150, "catalog phải có ~200 mục, hiện: " + catalog.size());
    }

    @Test
    void resolvesAliasToCanonicalNormalizedName() {
        assertEquals("ca chua", catalog.resolve("Cà chua bi"));
        assertEquals("ca chua", catalog.resolve("tomato"));
        assertEquals("dau hu", catalog.resolve("đậu phụ"));
        assertEquals("trung ga", catalog.resolve("Trứng"));
    }

    @Test
    void unknownIngredientFallsBackToNormalizedInput() {
        assertEquals("thanh long", catalog.resolve("Thanh Long"));
    }

    @Test
    void lookupReturnsFullEntryWithMetadata() {
        IngredientEntry tom = catalog.lookup("tôm sú").orElseThrow();
        assertEquals("tom", tom.normalizedName());
        assertEquals("hai_san", tom.category());
        assertEquals("hai_san", tom.allergenGroup());
        assertEquals(2, tom.shelfLifeDays());
    }

    @Test
    void lookupUnknownReturnsEmpty() {
        assertTrue(catalog.lookup("nguyên liệu không tồn tại xyz").isEmpty());
    }

    @Test
    void meVsMeChuaDoNotCollide() {
        // "mè" (vừng) và "me chua" (tamarind) phải phân biệt được sau normalize
        assertEquals("me", catalog.resolve("mè"));
        assertEquals("me", catalog.resolve("vừng"));
        assertEquals("me chua", catalog.resolve("me chua"));
    }

    @Test
    void allNormalizedNamesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (IngredientEntry e : catalog.entries()) {
            assertTrue(seen.add(e.normalizedName()),
                    "normalized_name trùng: " + e.normalizedName());
        }
    }

    @Test
    void everyNormalizedNameMatchesNormalizerOutput() {
        // normalized_name khai báo trong JSON phải khớp đúng output của normalizer
        for (IngredientEntry e : catalog.entries()) {
            assertEquals(e.normalizedName(), catalog.resolve(e.canonicalName()),
                    "canonical '" + e.canonicalName() + "' không resolve về normalized_name khai báo");
        }
    }
}
