package com.dishcover.image.config;

import com.dishcover.common.ingredient.IngredientCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Đăng ký {@link IngredientCatalog} làm Spring bean dùng chung cho toàn Image Service. */
@Configuration
public class CatalogConfig {

    /** IngredientCatalog là plain class trong common (không phải Spring bean) — load 1 lần, dùng chung toàn service. */
    @Bean
    IngredientCatalog ingredientCatalog() {
        return IngredientCatalog.loadDefault();
    }
}
