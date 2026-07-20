package com.dishcover.common.text;

import org.junit.jupiter.api.Test;

import static com.dishcover.common.text.VietnameseTextNormalizer.normalize;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VietnameseTextNormalizerTest {

    @Test
    void stripsDiacriticsAndLowercases() {
        assertEquals("ca chua bi", normalize("Cà Chua Bi"));
        assertEquals("nuoc mam", normalize("Nước Mắm"));
    }

    @Test
    void handlesDCharacterSpecially() {
        assertEquals("dau hu", normalize("Đậu hũ"));
        assertEquals("dua hau", normalize("Dưa hấu"));
    }

    @Test
    void removesPunctuationAndCollapsesWhitespace() {
        assertEquals("ca chua bi", normalize("  Cà chua (bi)  "));
        assertEquals("hanh la", normalize("hành,   lá"));
    }

    @Test
    void nullOrBlankReturnsEmpty() {
        assertEquals("", normalize(null));
        assertEquals("", normalize("   "));
    }

    @Test
    void isIdempotent() {
        assertEquals("ca chua", normalize(normalize("Cà chua")));
    }
}
