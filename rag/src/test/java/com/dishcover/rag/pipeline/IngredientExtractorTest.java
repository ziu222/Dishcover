package com.dishcover.rag.pipeline;

import com.dishcover.common.ingredient.IngredientCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ví dụ chạy tay đầy đủ ở specs/rag-service.md mục 3.1 — test này verify đúng kết quả cuối cùng. */
class IngredientExtractorTest {

    private final IngredientExtractor extractor = new IngredientExtractor(IngredientCatalog.loadDefault());

    @Test
    void extractsMultiWordAliasAndSkipsWordsOutsideCatalog() {
        // Cố ý dùng "nhà" thay vì "tôi" -- "tôi" normalize trùng "tỏi" (cùng về "toi"), xem
        // cảnh báo specs/rag-service.md mục 3.1 ngay sau bảng ví dụ chạy tay.
        List<String> result = extractor.extract(
                "nhà có trứng gà và cà chua bi, còn dư ít hành lá, nấu được món gì?");

        assertEquals(List.of("trung ga", "ca chua", "hanh la"), result);
    }

    @Test
    void toiPronounCollidesWithToiGarlicAfterNormalization() {
        // Chứng minh phát hiện ghi trong spec mục 3.1: "tôi" (đại từ) và "tỏi" (gia vị) cùng
        // normalize về "toi" -- câu hỏi chỉ có đại từ "tôi", không hề nhắc tỏi, vẫn ra "toi".
        List<String> result = extractor.extract("tôi thích nấu ăn");
        assertEquals(List.of("toi"), result);
    }

    @Test
    void noIngredientMentionedReturnsEmpty() {
        List<String> result = extractor.extract("hôm nay trời đẹp quá");
        assertTrue(result.isEmpty());
    }

    @Test
    void blankQuestionReturnsEmpty() {
        assertTrue(extractor.extract("   ").isEmpty());
    }

    @Test
    void singleWordAloneDoesNotMatchMultiWordCanonicalNameWithoutThatAlias() {
        // "cà" một mình không khớp "ca chua" -- catalog chỉ có alias "cà chua bi"/"tomato" cho
        // "Cà chua", không có alias "cà" đứng riêng (giới hạn đã biết, specs/rag-service.md mục 3.1)
        List<String> result = extractor.extract("nhà có cà, nấu gì được?");
        assertTrue(result.isEmpty());
    }
}
