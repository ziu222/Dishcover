package com.dishcover.common.nutrition;

import com.dishcover.common.ingredient.IngredientEntry;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Quy đổi (amount, unit) trên 1 dòng nguyên liệu sang gram, dùng chung cho
 * {@link RecipeNutritionCalculator} (tính calo) và Matching/Inventory Service (so sánh số lượng
 * đủ/thiếu, trừ kho — CLAUDE.md phần "hạ tầng dùng chung").
 *
 * <p>2 tầng tra cứu: (1) đơn vị đo lường chung (g/kg/ml/muỗng/chén...) áp dụng cho MỌI nguyên liệu,
 * quy đổi thể tích→gram giả định tỉ trọng ~1g/ml (ponytail: xấp xỉ cho chất lỏng thông thường, nước
 * mắm/dầu ăn lệch tỉ trọng thật nhưng sai số chấp nhận được ở lượng dùng nhỏ — nâng cấp per-ingredient
 * density nếu cần chính xác hơn); (2) đơn vị ĐẾM riêng của từng nguyên liệu (quả/củ/tép...), tra
 * trong {@code entry.unitToGram()} vì "1 quả" khác nhau tùy nguyên liệu.
 *
 * <p>Không quy đổi được (unit lạ, VD "chopped"/"to taste" từ dữ liệu TheMealDB) → trả
 * {@link Optional#empty()}, KHÔNG ném lỗi — gọi ý này cố ý để {@link RecipeNutritionCalculator}
 * đánh dấu {@code incomplete} thay vì chặn toàn bộ công thức.
 */
public final class UnitConverter {

    private static final Map<String, Double> GENERIC_UNIT_TO_GRAM = Map.ofEntries(
            Map.entry("g", 1.0),
            Map.entry("gram", 1.0),
            Map.entry("grams", 1.0),
            Map.entry("kg", 1000.0),
            Map.entry("ml", 1.0),
            Map.entry("l", 1000.0),
            Map.entry("lít", 1000.0),
            Map.entry("oz", 28.35),
            Map.entry("lb", 453.6),
            Map.entry("tsp", 5.0),
            Map.entry("teaspoon", 5.0),
            Map.entry("teaspoons", 5.0),
            Map.entry("muỗng cà phê", 5.0),
            Map.entry("tbsp", 15.0),
            Map.entry("tbs", 15.0),
            Map.entry("tblsp", 15.0),
            Map.entry("tbls", 15.0),
            Map.entry("tablespoon", 15.0),
            Map.entry("tablespoons", 15.0),
            Map.entry("muỗng canh", 15.0),
            Map.entry("cup", 240.0),
            Map.entry("cups", 240.0),
            Map.entry("chén", 200.0)
    );

    private UnitConverter() {
    }

    /**
     * Tra hệ số gram-trên-1-đơn-vị cho {@code unit} — đơn vị đo lường chung tra trước, đơn vị đếm
     * riêng của {@code entry} tra sau. Dùng riêng (ngoài {@link #toGrams}) khi cần quy đổi NGƯỢC
     * (VD số gram thiếu → số đơn vị gốc của công thức, xem Matching Service availability endpoint).
     */
    public static Optional<Double> gramsPerUnit(String unit, IngredientEntry entry) {
        if (unit == null) {
            return Optional.empty();
        }
        String key = unit.trim().toLowerCase(Locale.ROOT);
        Double perUnit = GENERIC_UNIT_TO_GRAM.get(key);
        if (perUnit == null && entry != null && entry.unitToGram() != null) {
            perUnit = entry.unitToGram().get(unit.trim());
        }
        return Optional.ofNullable(perUnit);
    }

    /** Quy đổi {@code amount} theo {@code unit} sang gram, tra thêm {@code entry} cho đơn vị đếm. */
    public static Optional<Double> toGrams(Double amount, String unit, IngredientEntry entry) {
        if (amount == null) {
            return Optional.empty();
        }
        return gramsPerUnit(unit, entry).map(perUnit -> amount * perUnit);
    }
}
