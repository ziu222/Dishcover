package com.dishcover.matching.config;

import com.dishcover.common.ingredient.IngredientCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cấu hình bean {@link IngredientCatalog} — từ điển nguyên liệu chuẩn hóa dùng chung toàn service. */
@Configuration
public class CatalogConfig {

    /** IngredientCatalog là plain class trong common (không phải Spring bean) — load 1 lần, dùng chung toàn service. */
    @Bean
    IngredientCatalog ingredientCatalog() {
        return IngredientCatalog.loadDefault();
    }
}
