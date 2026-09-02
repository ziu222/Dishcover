package com.dishcover.recipe.config;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.nutrition.RecipeNutritionCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Đăng ký {@link IngredientCatalog}/{@link RecipeNutritionCalculator} làm Spring bean dùng chung. */
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

    /**
     * @param catalog catalog nguyên liệu chuẩn dùng để tra calo/macro/quy đổi đơn vị
     * @return calculator tính calo/macro mỗi khẩu phần từ danh sách nguyên liệu 1 công thức
     */
    @Bean
    RecipeNutritionCalculator recipeNutritionCalculator(IngredientCatalog catalog) {
        return new RecipeNutritionCalculator(catalog);
    }
}
