package com.dishcover.rag.config;

import com.dishcover.common.ingredient.IngredientCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogConfig {

    /** IngredientCatalog là plain class trong common (không phải Spring bean) — load 1 lần, dùng chung toàn service. */
    @Bean
    IngredientCatalog ingredientCatalog() {
        return IngredientCatalog.loadDefault();
    }
}
