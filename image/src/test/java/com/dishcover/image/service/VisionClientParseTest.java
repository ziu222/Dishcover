package com.dishcover.image.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionClientParseTest {

    @Test
    void parsesPlainJsonArray() {
        String raw = "[{\"name\":\"Trứng gà\",\"confidence\":0.9,\"quantity_guess\":\"2 quả\"}]";

        List<RawRecognizedItem> items = VisionClient.parseJsonArrayLenient(raw);

        assertEquals(1, items.size());
        assertEquals("Trứng gà", items.get(0).name());
        assertEquals(0.9, items.get(0).confidence());
        assertEquals("2 quả", items.get(0).quantityGuess());
    }

    @Test
    void stripsMarkdownCodeFence() {
        String raw = "```json\n[{\"name\":\"Cà chua\",\"confidence\":0.8,\"quantity_guess\":null}]\n```";

        List<RawRecognizedItem> items = VisionClient.parseJsonArrayLenient(raw);

        assertEquals(1, items.size());
        assertEquals("Cà chua", items.get(0).name());
    }

    @Test
    void ignoresSurroundingProse() {
        String raw = "Đây là các nguyên liệu tôi thấy: [{\"name\":\"Hành lá\",\"confidence\":0.7,"
                + "\"quantity_guess\":\"1 nhánh\"}] Hy vọng giúp ích!";

        List<RawRecognizedItem> items = VisionClient.parseJsonArrayLenient(raw);

        assertEquals(1, items.size());
        assertEquals("Hành lá", items.get(0).name());
    }

    @Test
    void emptyArrayMeansNoIngredients() {
        assertTrue(VisionClient.parseJsonArrayLenient("[]").isEmpty());
    }

    @Test
    void throwsWhenNoArrayPresent() {
        assertThrows(IllegalStateException.class,
                () -> VisionClient.parseJsonArrayLenient("Xin lỗi, tôi không nhìn thấy gì cả."));
    }

    @Test
    void throwsOnBlank() {
        assertThrows(IllegalStateException.class,
                () -> VisionClient.parseJsonArrayLenient("   "));
    }
}
