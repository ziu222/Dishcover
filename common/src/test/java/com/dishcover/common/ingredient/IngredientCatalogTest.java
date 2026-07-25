package com.dishcover.common.ingredient;

import com.dishcover.common.text.VietnameseTextNormalizer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        // So TRỰC TIẾP với normalizer, không qua resolve() — resolve() luôn trả về normalizedName
        // của chính entry đó (constructor đã index canonicalName) nên không kiểm được JSON viết sai.
        for (IngredientEntry e : catalog.entries()) {
            assertEquals(e.normalizedName(), VietnameseTextNormalizer.normalize(e.canonicalName()),
                    "normalized_name khai báo không khớp normalize(canonicalName) ở '" + e.canonicalName() + "'");
        }
    }

    @Test
    void noAliasCollisionBetweenDifferentIngredients() {
        // Guard cho bug đã xảy ra: "ngô" (bắp) và "ngò" (rau mùi) cùng normalize thành "ngo".
        // Load thành công = không có tranh chấp (constructor ném lỗi nếu có), nhưng kiểm lại tường minh
        // để thông báo lỗi rõ ràng hơn khi ai đó thêm alias mới.
        Map<String, String> owner = new HashMap<>();
        for (IngredientEntry e : catalog.entries()) {
            List<String> keys = new ArrayList<>(List.of(e.canonicalName(), e.normalizedName()));
            if (e.aliases() != null) {
                keys.addAll(e.aliases());
            }
            for (String raw : keys) {
                String key = VietnameseTextNormalizer.normalize(raw);
                if (key.isEmpty()) {
                    continue;
                }
                String prev = owner.putIfAbsent(key, e.normalizedName());
                assertTrue(prev == null || prev.equals(e.normalizedName()),
                        "Khóa '" + key + "' (từ '" + raw + "') bị tranh chấp giữa '"
                                + prev + "' và '" + e.normalizedName() + "'");
            }
        }
    }

    @Test
    void coconutIsReachableByFullName() {
        // "dừa"/"dứa"/"dưa" đều normalize thành "dua" — giới hạn cố hữu của bỏ dấu, không tách được.
        // Chấp nhận: khóa "dua" thuộc về Dứa (cách gõ trần phổ biến nhất);
        // dừa phải tra bằng tên đầy đủ, và các dạng dùng thật của dừa đều có entry riêng.
        assertEquals("cui dua", catalog.resolve("cùi dừa"));
        assertEquals("cui dua", catalog.resolve("dừa nạo"));
        assertEquals("cui dua", catalog.resolve("coconut"));
        assertEquals("nuoc cot dua", catalog.resolve("nước cốt dừa"));
        assertEquals("nuoc dua", catalog.resolve("nước dừa"));
        assertEquals("dua", catalog.resolve("dứa"));
    }

    @Test
    void ngoDoesNotResolveToCorn() {
        // Regression: trước đây resolve("ngò") trả "bap" (Bắp) do collision
        assertEquals("bap", catalog.resolve("ngô"), "'ngô' vẫn phải ra Bắp");
        assertEquals("ngo ri", catalog.resolve("rau mùi"), "'rau mùi' phải ra Ngò rí");
        assertEquals("ngo ri", catalog.resolve("Ngò rí"));
    }
}
