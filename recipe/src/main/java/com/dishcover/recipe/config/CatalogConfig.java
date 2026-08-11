package com.dishcover.recipe.config;

import com.dishcover.common.ingredient.IngredientCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Đăng ký {@link IngredientCatalog} làm Spring bean để dùng chung trong toàn Recipe Service. */
@Configuration
public class CatalogConfig {

    /**
     * IngredientCatalog là plain class trong common (không phải Spring bean) — load 1 lần, dùng chung toàn service.
     *
     * @return catalog nguyên liệu chuẩn đã nạp từ dữ liệu tĩnh mặc định trong module {@code common}
     */
    @Bean
    IngredientCatalog ingredientCatalog() {
        return IngredientCatalog.loadDefault();
    }
}
