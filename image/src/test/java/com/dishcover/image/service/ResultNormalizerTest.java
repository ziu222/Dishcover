package com.dishcover.image.service;

import com.dishcover.common.ingredient.DefaultShelfLifeTable;
import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.image.dto.ImageDtos.RecognizedIngredientDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultNormalizerTest {

    private final ResultNormalizer normalizer = new ResultNormalizer(IngredientCatalog.loadDefault());
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    @Test
    void mapsAliasNameToCanonicalNormalizedNameAndExactShelfLife() {
        // "trứng" là alias của "Trứng gà" (normalized "trung ga", shelfLifeDays=21)
        var raw = new RawRecognizedItem("trứng", 0.95, "3 quả");

        List<RecognizedIngredientDto> result = normalizer.normalize(List.of(raw), TODAY);

        RecognizedIngredientDto dto = result.get(0);
        assertEquals("trứng", dto.name());            // giữ tên hiển thị model trả
        assertEquals("trung ga", dto.normalizedName()); // khóa so khớp canonical
        assertEquals(0.95, dto.confidence());
        assertEquals("3 quả", dto.quantityGuess());
        assertEquals(TODAY.plusDays(21), dto.suggestedExpiryDate());
    }

    @Test
    void unknownIngredientFallsBackToGlobalDefaultShelfLife() {
        var raw = new RawRecognizedItem("nguyên liệu lạ không có trong catalog", 0.4, null);

        List<RecognizedIngredientDto> result = normalizer.normalize(List.of(raw), TODAY);

        RecognizedIngredientDto dto = result.get(0);
        // Không có trong catalog -> normalizedName là chuỗi đã normalize, hạn dùng = default toàn cục
        assertEquals(TODAY.plusDays(DefaultShelfLifeTable.GLOBAL_FALLBACK_DAYS), dto.suggestedExpiryDate());
    }

    @Test
    void emptyInputYieldsEmptyOutput() {
        assertEquals(List.of(), normalizer.normalize(List.of(), TODAY));
    }
}
