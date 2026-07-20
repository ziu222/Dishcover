package com.dishcover.common.ingredient;

import java.util.Map;
import java.util.Set;

/**
 * Bảng hạn dùng mặc định theo category (CLAUDE.md mục 7).
 * Dùng làm fallback thô khi không có shelf_life_days chính xác cho từng nguyên liệu:
 * ưu tiên IngredientCatalog.lookup().shelfLifeDays trước, không có mới rơi về đây.
 * Chủ yếu phục vụ Image Recognition khi gặp nguyên liệu lạ ngoài catalog.
 */
public final class DefaultShelfLifeTable {

    /** Số ngày dùng khi không xác định được category. */
    public static final int GLOBAL_FALLBACK_DAYS = 7;

    private static final Map<String, Integer> BY_CATEGORY = Map.ofEntries(
            Map.entry("la_thom", 5),      // rau lá thơm: 3-5 ngày
            Map.entry("rau_cu", 10),      // rau củ quả: 7-14 ngày
            Map.entry("nam", 5),          // nấm tươi
            Map.entry("thit", 3),         // thịt tươi: 2-3 ngày
            Map.entry("hai_san", 2),      // hải sản tươi
            Map.entry("trung_sua", 14),   // trứng 21, sữa 7-14
            Map.entry("trai_cay", 7),     // trái cây
            Map.entry("tinh_bot", 30),    // gạo/mì khô lâu, bún/phở tươi ngắn — lấy trung bình thô
            Map.entry("dau_hat", 90),     // đậu/hạt khô lâu, đậu hũ ngắn — lấy trung bình thô
            Map.entry("gia_vi", 180),     // gia vị/nước chấm
            Map.entry("do_kho", 180)      // đồ khô/đóng hộp
    );

    private DefaultShelfLifeTable() {
    }

    /** Tập category có default riêng — dùng để guard: catalog không được thêm category lạ mà quên bảng này. */
    public static Set<String> knownCategories() {
        return BY_CATEGORY.keySet();
    }

    /** Hạn dùng mặc định cho 1 category; category lạ/null → GLOBAL_FALLBACK_DAYS. */
    public static int forCategory(String category) {
        if (category == null) {
            return GLOBAL_FALLBACK_DAYS;
        }
        return BY_CATEGORY.getOrDefault(category, GLOBAL_FALLBACK_DAYS);
    }
}
