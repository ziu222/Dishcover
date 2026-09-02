package com.dishcover.common.nutrition;

import com.dishcover.common.ingredient.IngredientCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeNutritionCalculatorTest {

    private final IngredientCatalog catalog = IngredientCatalog.loadDefault();
    private final RecipeNutritionCalculator calculator = new RecipeNutritionCalculator(catalog);

    @Test
    void computesCaloriesPerServingFromCleanUnits() {
        // Trứng chiên cà chua thật (seed vn): 3 quả trứng gà + 2 quả cà chua, chia 2 khẩu phần
        RecipeNutrition result = calculator.calculate(List.of(
                new NutritionIngredientLine("trung ga", 3.0, "quả"),
                new NutritionIngredientLine("ca chua", 2.0, "quả")
        ), 2);

        // trứng: 3*50g=150g -> 232.5 kcal; cà chua: 2*100g=200g -> 36 kcal; tổng 268.5, chia 2 = 134.25
        assertEquals(134.3, result.caloriesPerServing(), 0.5);
        assertFalse(result.incomplete());
    }

    @Test
    void unresolvableUnitMarksIncompleteButKeepsPartialTotal() {
        RecipeNutrition result = calculator.calculate(List.of(
                new NutritionIngredientLine("trung ga", 2.0, "quả"),
                new NutritionIngredientLine("hanh la", 1.0, "chopped") // unit lạ, không quy đổi được
        ), 1);

        assertTrue(result.incomplete());
        assertTrue(result.caloriesPerServing() > 0, "vẫn tính được phần trứng dù hành lá không quy đổi được");
    }

    @Test
    void unknownIngredientMarksIncomplete() {
        RecipeNutrition result = calculator.calculate(List.of(
                new NutritionIngredientLine("nguyen lieu khong ton tai xyz", 100.0, "g")
        ), 1);

        assertTrue(result.incomplete());
        assertEquals(0, result.caloriesPerServing());
    }

    @Test
    void missingServingsDefaultsToOne() {
        RecipeNutrition withNull = calculator.calculate(
                List.of(new NutritionIngredientLine("gao", 100.0, "g")), null);
        RecipeNutrition withOne = calculator.calculate(
                List.of(new NutritionIngredientLine("gao", 100.0, "g")), 1);

        assertEquals(withOne.caloriesPerServing(), withNull.caloriesPerServing());
    }

    @Test
    void genericUnitsConvertAcrossAnyIngredient() {
        // 15g muối = 0 kcal (kiểm tra tsp/g generic không phụ thuộc unitToGram riêng)
        RecipeNutrition result = calculator.calculate(List.of(
                new NutritionIngredientLine("dau an", 1.0, "tbsp")
        ), 1);

        // 1 tbsp = 15g dầu ăn (884 kcal/100g) = 132.6 kcal
        assertEquals(132.6, result.caloriesPerServing(), 0.5);
        assertFalse(result.incomplete());
    }
}
