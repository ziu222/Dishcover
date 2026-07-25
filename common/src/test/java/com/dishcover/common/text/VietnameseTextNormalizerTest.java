package com.dishcover.common.text;

import org.junit.jupiter.api.Test;

import java.util.Locale;

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

    @Test
    void unaffectedByTurkishLocale() {
        // toLowerCase() theo locale mặc định biến 'I' thành 'ı' (i không chấm) trên locale tr/az,
        // khiến "ICING"/"Ginger" bị băm nát. Test này fail nếu ai bỏ Locale.ROOT.
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("icing sugar", normalize("ICING Sugar"));
            assertEquals("ginger", normalize("GINGER"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
