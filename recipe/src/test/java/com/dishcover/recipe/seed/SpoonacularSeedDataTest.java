package com.dishcover.recipe.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guard cho seed Spoonacular (fetch qua {@code scripts/fetch-spoonacular.mjs}). Khác 2 batch
 * kia: KHÔNG ép mọi normalized_name phải có trong catalog (ingredient tiếng Anh, coverage thấp —
 * tradeoff đã biết, giống TheMealDB), nhưng batch này PHẢI có sẵn {@code nutrition}/{@code servings}
 * thật từ API (không để RecipeSeeder tự tính bù — xem specs/diet-direction-recommendation.md mục 7.4).
 */
class SpoonacularSeedDataTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode load() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/seed/recipes-spoonacular.json")) {
            assertNotNull(in, "Không tìm thấy seed/recipes-spoonacular.json");
            return mapper.readTree(in);
        }
    }

    @Test
    void schemaAndConventionsHold() throws Exception {
        JsonNode recipes = load();
        assertTrue(recipes.size() > 0, "seed Spoonacular không được rỗng");
        Set<String> ids = new HashSet<>();
        for (JsonNode r : recipes) {
            for (String field : new String[]{"_id", "name", "slug", "difficulty", "ingredients", "steps"}) {
                assertTrue(r.hasNonNull(field), "thiếu field '" + field + "' ở " + r.path("name").asText());
            }
            assertTrue(r.get("_id").asText().startsWith("spoon_"), "id Spoonacular phải prefix spoon_");
            assertTrue(ids.add(r.get("_id").asText()), "trùng _id: " + r.get("_id").asText());
            assertTrue(r.get("ingredients").size() > 0);
            assertTrue(r.get("steps").size() > 0, "món '" + r.get("name").asText() + "' thiếu steps");
            for (JsonNode ing : r.get("ingredients")) {
                assertFalse(ing.get("normalized_name").asText().isEmpty(), "normalized_name rỗng");
                boolean essential = ing.get("essential").asBoolean();
                double weight = ing.get("weight").asDouble();
                assertEquals(essential ? 1.0 : 0.3, weight, 0.001,
                        "quy ước essential/weight sai ở " + ing.get("name").asText());
            }
        }
    }

    @Test
    void nutritionAndServingsComeFromApiNotComputed() throws Exception {
        // Đặc thù riêng batch này (khác vn/au/themealdb): phải có sẵn trong JSON, RecipeSeeder
        // KHÔNG được tự tính bù (xem RecipeSeeder.loadAll() — chỉ compute khi thiếu).
        for (JsonNode r : load()) {
            assertTrue(r.hasNonNull("servings"), "món '" + r.get("name").asText() + "' thiếu servings sẵn từ API");
            JsonNode n = r.get("nutrition");
            assertNotNull(n, "món '" + r.get("name").asText() + "' thiếu nutrition sẵn từ API");
            assertTrue(n.hasNonNull("caloriesPerServing"));
            assertFalse(n.get("incomplete").asBoolean(), "nutrition Spoonacular phải incomplete=false");
        }
    }

    @Test
    void idsDoNotClashWithOtherBatches() throws Exception {
        Set<String> otherIds = new HashSet<>();
        for (String path : new String[]{"/seed/recipes-vn.json", "/seed/recipes-au.json", "/seed/recipes-themealdb.json"}) {
            try (InputStream in = getClass().getResourceAsStream(path)) {
                for (JsonNode r : mapper.readTree(in)) {
                    otherIds.add(r.get("_id").asText());
                }
            }
        }
        for (JsonNode r : load()) {
            assertFalse(otherIds.contains(r.get("_id").asText()), "_id đụng batch khác: " + r.get("_id").asText());
        }
    }
}
