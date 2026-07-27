package com.dishcover.rag.pipeline;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;
import com.dishcover.common.text.VietnameseTextNormalizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ví dụ chạy tay đầy đủ ở specs/rag-service.md mục 3.1 — test này verify đúng kết quả cuối cùng. */
class IngredientExtractorTest {

    private final IngredientCatalog catalog = IngredientCatalog.loadDefault();
    private final IngredientExtractor extractor = new IngredientExtractor(catalog);

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

    @Test
    void noCatalogAliasExceedsMaxWindow() {
        // Guard test (phát hiện lúc code-reviewer subagent verify): nếu sau này ai thêm 1 alias/tên
        // dài hơn MAX_WINDOW từ vào catalog, thuật toán sẽ ÂM THẦM không khớp được (không lỗi, chỉ
        // thiếu sót) -- test này fail ngay lúc build thay vì im lặng bỏ sót lúc chạy thật.
        for (IngredientEntry entry : catalog.entries()) {
            assertTrue(tokenCount(entry.canonicalName()) <= IngredientExtractor.MAX_WINDOW,
                    "canonicalName vượt MAX_WINDOW: " + entry.canonicalName());
            assertTrue(tokenCount(entry.normalizedName()) <= IngredientExtractor.MAX_WINDOW,
                    "normalizedName vượt MAX_WINDOW: " + entry.normalizedName());
            if (entry.aliases() != null) {
                for (String alias : entry.aliases()) {
                    assertTrue(tokenCount(alias) <= IngredientExtractor.MAX_WINDOW,
                            "alias vượt MAX_WINDOW: " + alias + " (của " + entry.canonicalName() + ")");
                }
            }
        }
    }

    private int tokenCount(String raw) {
        String normalized = VietnameseTextNormalizer.normalize(raw);
        return normalized.isEmpty() ? 0 : normalized.split(" ").length;
    }
}
