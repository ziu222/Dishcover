package com.dishcover.inventory.controller;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Danh sách tên nguyên liệu chuẩn (~200 mục) để FE gợi ý (autocomplete) khi người dùng gõ tên
 * nguyên liệu — tránh gõ sai chính tả/trùng lặp nguyên liệu đã có trong catalog. Dữ liệu tĩnh,
 * chỉ yêu cầu JWT hợp lệ như mọi endpoint khác của Inventory Service, không có business logic
 * nào khác nên không cần tách service riêng.
 */
@RestController
@RequestMapping("/inventory/catalog")
public class IngredientCatalogController {

    private final List<String> ingredientNames;

    public IngredientCatalogController(IngredientCatalog catalog) {
        this.ingredientNames = catalog.entries().stream()
                .map(IngredientEntry::canonicalName)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    /** @return tên hiển thị (canonical, có dấu) của mọi nguyên liệu trong catalog, đã sắp xếp A-Z */
    @GetMapping("/ingredients")
    public List<String> ingredients() {
        return ingredientNames;
    }
}
