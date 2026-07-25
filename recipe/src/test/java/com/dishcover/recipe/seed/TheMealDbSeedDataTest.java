package com.dishcover.recipe.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

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

    /** Từ vựng dietary_flags được phép — dùng chung cho cả 2 nguồn seed để không trôi mỗi nơi một kiểu. */
    private static final Set<String> ALLOWED_FLAGS = Set.of(
            "contains_meat", "contains_seafood", "contains_egg", "contains_dairy",
            "contains_gluten", "contains_nuts", "contains_sesame", "vegetarian", "vegan");

    private static final Pattern MEAT = Pattern.compile(
            "\\b(beef|pork|lamb|mutton|bacon|ham|chicken|turkey|duck|steak|mince|minced|sausage|chorizo|veal|liver)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SEAFOOD = Pattern.compile(
            "\\b(fish|prawn|prawns|shrimp|crab|squid|clam|mussel|oyster|scallop|salmon|tuna|cod|mackerel|anchovy|anchovies)\\b",
            Pattern.CASE_INSENSITIVE);

    @Test
    void flagsUseAllowedVocabularyOnly() throws Exception {
        for (String path : new String[]{"/seed/recipes-themealdb.json", "/seed/recipes-vn.json"}) {
            for (JsonNode r : load(path)) {
                for (JsonNode flag : r.path("dietary_flags")) {
                    assertTrue(ALLOWED_FLAGS.contains(flag.asText()),
                            "cờ lạ '" + flag.asText() + "' ở món " + r.get("name").asText() + " (" + path + ")");
                }
            }
        }
    }

    @Test
    void dishesWithMeatOrSeafoodAreFlagged() throws Exception {
        // Bỏ sót cờ ở đây nghĩa là gợi ý món thịt/hải sản cho người ăn chay hoặc dị ứng —
        // từng xảy ra: "Vietnamese lamb shanks" thiếu contains_meat, "Bang bang prawn salad" thiếu contains_seafood.
        for (JsonNode r : load("/seed/recipes-themealdb.json")) {
            Set<String> flags = new HashSet<>();
            r.path("dietary_flags").forEach(f -> flags.add(f.asText()));
            for (JsonNode ing : r.get("ingredients")) {
                String name = ing.get("name").asText();
                if (MEAT.matcher(name).find()) {
                    assertTrue(flags.contains("contains_meat"),
                            "món '" + r.get("name").asText() + "' có '" + name + "' nhưng thiếu contains_meat");
                }
                if (SEAFOOD.matcher(name).find()) {
                    assertTrue(flags.contains("contains_seafood"),
                            "món '" + r.get("name").asText() + "' có '" + name + "' nhưng thiếu contains_seafood");
                }
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
