package com.dishcover.recipe.seed;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guard chất lượng dữ liệu seed: đảm bảo mọi công thức khớp schema mục 3.2 và
 * normalized_name của nguyên liệu tra được trong catalog (nếu không sẽ không match với tủ lạnh người dùng).
 */
class RecipeSeedDataTest {

    private final IngredientCatalog catalog = IngredientCatalog.loadDefault();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode loadVnRecipes() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/seed/recipes-vn.json")) {
            assertNotNull(in, "Không tìm thấy seed/recipes-vn.json");
            return mapper.readTree(in);
        }
    }

    @Test
    void everyIngredientNormalizedNameIsInCatalog() throws Exception {
        for (JsonNode recipe : loadVnRecipes()) {
            for (JsonNode ing : recipe.get("ingredients")) {
                String norm = ing.get("normalized_name").asText();
                assertTrue(catalog.lookup(norm).isPresent(),
                        "normalized_name '" + norm + "' (món " + recipe.get("name").asText()
                                + ") không có trong catalog → sẽ không match được tủ lạnh người dùng");
            }
        }
    }

    @Test
    void ingredientNameResolvesToItsDeclaredNormalizedName() throws Exception {
        // Nếu tên hiển thị không tự resolve về chính normalized_name của nó thì user gõ đúng tên đó
        // vẫn không match được công thức. Từng lệch ở "Thịt ba chỉ" và "Cơm nguội".
        for (JsonNode recipe : loadVnRecipes()) {
            for (JsonNode ing : recipe.get("ingredients")) {
                String name = ing.get("name").asText();
                assertEquals(ing.get("normalized_name").asText(), catalog.resolve(name),
                        "tên '" + name + "' (món " + recipe.get("name").asText()
                                + ") không resolve về normalized_name khai báo → user gõ tên này sẽ không match");
            }
        }
    }

    @Test
    void essentialWeightConventionHolds() throws Exception {
        for (JsonNode recipe : loadVnRecipes()) {
            boolean hasEssential = false;
            for (JsonNode ing : recipe.get("ingredients")) {
                boolean essential = ing.get("essential").asBoolean();
                double weight = ing.get("weight").asDouble();
                if (essential) {
                    hasEssential = true;
                    assertEquals(1.0, weight, 0.001,
                            "nguyên liệu essential phải weight=1.0: " + ing.get("name").asText());
                } else {
                    assertEquals(0.3, weight, 0.001,
                            "nguyên liệu phụ phải weight=0.3: " + ing.get("name").asText());
                }
            }
            assertTrue(hasEssential,
                    "món '" + recipe.get("name").asText() + "' phải có ít nhất 1 nguyên liệu essential");
        }
    }

    @Test
    void requiredFieldsPresentAndUniqueIds() throws Exception {
        JsonNode recipes = loadVnRecipes();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (JsonNode r : recipes) {
            for (String field : new String[]{"_id", "name", "slug", "cook_time_minutes", "difficulty", "ingredients", "steps"}) {
                assertTrue(r.hasNonNull(field), "thiếu field '" + field + "' ở món " + r.path("name").asText());
            }
            assertTrue(r.get("ingredients").size() > 0, "món phải có nguyên liệu");
            assertTrue(r.get("steps").size() > 0, "món phải có bước nấu");
            assertTrue(ids.add(r.get("_id").asText()), "trùng _id: " + r.get("_id").asText());
        }
        assertEquals(10, recipes.size(), "batch món Việt hiện có 10 công thức");
    }
}
