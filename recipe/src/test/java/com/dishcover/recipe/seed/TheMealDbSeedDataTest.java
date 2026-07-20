package com.dishcover.recipe.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guard cho seed TheMealDB (transform tự động). Khác batch món Việt: KHÔNG ép mọi normalized_name
 * phải có trong catalog (nguyên liệu ngoại nhiều cái không khớp — đây là tradeoff đã biết), chỉ đảm bảo
 * schema hợp lệ + quy ước essential/weight + id không trùng, và _id không đụng batch món Việt.
 */
class TheMealDbSeedDataTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode load(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, "Không tìm thấy " + path);
            return mapper.readTree(in);
        }
    }

    @Test
    void schemaAndConventionsHold() throws Exception {
        JsonNode recipes = load("/seed/recipes-themealdb.json");
        assertTrue(recipes.size() >= 40, "kỳ vọng ~50 công thức TheMealDB, có: " + recipes.size());
        Set<String> ids = new HashSet<>();
        for (JsonNode r : recipes) {
            for (String field : new String[]{"_id", "name", "slug", "difficulty", "ingredients", "steps"}) {
                assertTrue(r.hasNonNull(field), "thiếu field '" + field + "' ở " + r.path("name").asText());
            }
            assertTrue(r.get("_id").asText().startsWith("mealdb_"), "id TheMealDB phải prefix mealdb_");
            assertTrue(ids.add(r.get("_id").asText()), "trùng _id: " + r.get("_id").asText());
            assertTrue(r.get("ingredients").size() > 0);
            assertTrue(r.get("steps").size() > 0);
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
    void idsDoNotClashWithVietnameseBatch() throws Exception {
        Set<String> vnIds = new HashSet<>();
        for (JsonNode r : load("/seed/recipes-vn.json")) {
            vnIds.add(r.get("_id").asText());
        }
        for (JsonNode r : load("/seed/recipes-themealdb.json")) {
            assertFalse(vnIds.contains(r.get("_id").asText()),
                    "_id đụng batch món Việt: " + r.get("_id").asText());
        }
    }
}
