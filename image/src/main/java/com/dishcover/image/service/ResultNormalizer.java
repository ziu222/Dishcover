package com.dishcover.image.service;

import com.dishcover.common.ingredient.DefaultShelfLifeTable;
import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;
import com.dishcover.image.dto.ImageDtos.RecognizedIngredientDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Chuẩn hóa kết quả thô của Vision API qua Ingredient Catalog + bảng hạn dùng mặc định (đều đã có
 * sẵn trong {@code common}, KHÔNG viết lại). Điền {@code normalizedName} (khóa so khớp với
 * Inventory) và {@code suggestedExpiryDate} gợi ý — người dùng sửa được, KHÔNG ghi DB
 * (human-in-the-loop, CLAUDE.md mục 7).
 */
@Component
public class ResultNormalizer {

    private final IngredientCatalog catalog;

    public ResultNormalizer(IngredientCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * @param rawItems danh sách nguyên liệu thô từ VisionClient
     * @param today    ngày tham chiếu để tính hạn dùng gợi ý (tách tham số cho test tất định)
     * @return danh sách DTO đã chuẩn hóa để client hiển thị màn xác nhận
     */
    public List<RecognizedIngredientDto> normalize(List<RawRecognizedItem> rawItems, LocalDate today) {
        return rawItems.stream()
                .map(item -> toDto(item, today))
                .toList();
    }

    private RecognizedIngredientDto toDto(RawRecognizedItem item, LocalDate today) {
        String normalizedName = catalog.resolve(item.name());
        Optional<IngredientEntry> entry = catalog.lookup(item.name());

        // Ưu tiên shelf_life_days chính xác của nguyên liệu; không có mới rơi về bảng theo category.
        Integer shelfDays = entry.map(IngredientEntry::shelfLifeDays).orElse(null);
        if (shelfDays == null) {
            String category = entry.map(IngredientEntry::category).orElse(null);
            shelfDays = DefaultShelfLifeTable.forCategory(category);
        }
        LocalDate suggestedExpiry = today.plusDays(shelfDays);

        return new RecognizedIngredientDto(
                item.name(), normalizedName, item.confidence(), item.quantityGuess(), suggestedExpiry);
    }
}
