package com.dishcover.matching.service;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;
import com.dishcover.matching.client.InventoryClient;
import com.dishcover.matching.client.RecipeClient;
import com.dishcover.matching.client.UserClient;
import com.dishcover.matching.dto.MatchingDtos.IngredientAvailabilityResponse;
import com.dishcover.matching.dto.MatchingDtos.RecipeAvailabilityResponse;
import com.dishcover.matching.dto.MatchingDtos.RecipeMatchResponse;
import com.dishcover.matching.scoring.AllergyFilterRule;
import com.dishcover.matching.scoring.EssentialWeightRule;
import com.dishcover.matching.scoring.ExpiryBonusRule;
import com.dishcover.matching.scoring.JaccardBaseRule;
import com.dishcover.matching.scoring.MatchingEngine;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Không dùng Spring context — mỗi client tự có RestClient.Builder riêng, bind MockRestServiceServer
 * trực tiếp lên đó (không gọi Inventory/Recipe/User thật khi chạy mvn test, specs/matching-service.md mục 6).
 */
class MatchingServiceTest {

    private static final String BEARER = "Bearer test-token";

    private MockRestServiceServer inventoryServer;
    private MockRestServiceServer recipeServer;
    private MockRestServiceServer userServer;

    private MatchingService buildService() {
        RestClient.Builder inventoryBuilder = RestClient.builder();
        inventoryServer = MockRestServiceServer.bindTo(inventoryBuilder).build();
        InventoryClient inventoryClient = new InventoryClient(inventoryBuilder, "http://inventory");

        RestClient.Builder recipeBuilder = RestClient.builder();
        recipeServer = MockRestServiceServer.bindTo(recipeBuilder).build();
        RecipeClient recipeClient = new RecipeClient(recipeBuilder, "http://recipe");

        RestClient.Builder userBuilder = RestClient.builder();
        userServer = MockRestServiceServer.bindTo(userBuilder).build();
        UserClient userClient = new UserClient(userBuilder, "http://user");

        IngredientCatalog catalog = new IngredientCatalog(List.of(
                IngredientEntry.basic("Trứng gà", "trung ga", List.of(), "dam_dong_vat", 21, "trung"),
                IngredientEntry.basic("Cà chua", "ca chua", List.of(), "rau_cu", 7, null),
                IngredientEntry.basic("Hành lá", "hanh la", List.of(), "rau_cu", 5, null),
                IngredientEntry.basic("Tôm", "tom", List.of(), "hai_san", 3, "hai_san")));
        MatchingEngine engine = new MatchingEngine(List.of(
                new JaccardBaseRule(), new EssentialWeightRule(), new ExpiryBonusRule(),
                new AllergyFilterRule(catalog)));

        return new MatchingService(inventoryClient, recipeClient, userClient, engine, catalog);
    }

    @Test
    void suggestScoresRanksAndExcludesAllergyViolations() {
        MatchingService service = buildService();

        inventoryServer.expect(requestTo("http://inventory/inventory/items"))
                .andExpect(header("Authorization", BEARER))
                .andRespond(withSuccess("""
                        [{"normalizedName":"trung ga","expiryDate":"%s","status":"FRESH"},
                         {"normalizedName":"sua tuoi","expiryDate":null,"status":"FRESH"},
                         {"normalizedName":"rau muong","expiryDate":null,"status":"FRESH"}]
                        """.formatted(LocalDate.now().plusDays(2)), MediaType.APPLICATION_JSON));

        userServer.expect(requestTo("http://user/users/me/dietary-preferences"))
                .andExpect(header("Authorization", BEARER))
                .andRespond(withSuccess("""
                        [{"id":1,"type":"ALLERGY","value":"hải sản"}]
                        """, MediaType.APPLICATION_JSON));
        userServer.expect(requestTo("http://user/users/me/calorie-goal"))
                .andExpect(header("Authorization", BEARER))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        recipeServer.expect(requestTo("http://recipe/recipes?size=500"))
                .andRespond(withSuccess("""
                        {"content":[{"id":"r1"},{"id":"r2"}]}
                        """, MediaType.APPLICATION_JSON));

        recipeServer.expect(requestTo("http://recipe/recipes/r1"))
                .andRespond(withSuccess("""
                        {"id":"r1","name":"Trứng chiên cà chua","slug":"trung-chien-ca-chua","imageUrl":null,
                         "ingredients":[{"name":"trung ga","normalizedName":"trung ga","essential":true,"weight":1.0},
                                        {"name":"ca chua","normalizedName":"ca chua","essential":true,"weight":1.0},
                                        {"name":"hanh la","normalizedName":"hanh la","essential":false,"weight":0.3}]}
                        """, MediaType.APPLICATION_JSON));

        recipeServer.expect(requestTo("http://recipe/recipes/r2"))
                .andRespond(withSuccess("""
                        {"id":"r2","name":"Tôm rang me","slug":"tom-rang-me","imageUrl":null,
                         "ingredients":[{"name":"tom","normalizedName":"tom","essential":true,"weight":1.0}]}
                        """, MediaType.APPLICATION_JSON));

        List<RecipeMatchResponse> result = service.suggest(BEARER, 5);

        // r2 chứa "tom" thuộc allergenGroup "hai_san" mà user dị ứng -> bị loại hoàn toàn khỏi kết quả
        assertEquals(1, result.size());
        assertEquals("r1", result.get(0).recipeId());
        assertEquals(List.of("trung ga"), result.get(0).matchedIngredients());
        assertEquals(List.of("ca chua", "hanh la"), result.get(0).missingIngredients());
        assertEquals(0.2 * (0.5 / 1.3) + 0.5, result.get(0).score(), 1e-9);

        inventoryServer.verify();
        recipeServer.verify();
        userServer.verify();
    }

    @Test
    void suggestClampsTopNWithinBounds() {
        MatchingService service = buildService();

        inventoryServer.expect(requestTo("http://inventory/inventory/items"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        userServer.expect(requestTo("http://user/users/me/dietary-preferences"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        userServer.expect(requestTo("http://user/users/me/calorie-goal"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        recipeServer.expect(requestTo("http://recipe/recipes?size=500"))
                .andRespond(withSuccess("{\"content\":[]}", MediaType.APPLICATION_JSON));

        List<RecipeMatchResponse> result = service.suggest(BEARER, 999);
        assertEquals(0, result.size()); // không có recipe nào -> rỗng, không lỗi dù topN vượt MAX_TOP_N
    }

    @Test
    void searchByIngredientsScoresIgnoringExpiryAndAllergyEvenIfSameNameElsewhereIsRestricted() {
        MatchingService service = buildService();

        // Không gọi Inventory/User ở đây -- searchByIngredients không cần bearerToken/user nào cả.
        recipeServer.expect(requestTo("http://recipe/recipes?size=500"))
                .andRespond(withSuccess("""
                        {"content":[{"id":"r1"},{"id":"r2"}]}
                        """, MediaType.APPLICATION_JSON));

        recipeServer.expect(requestTo("http://recipe/recipes/r1"))
                .andRespond(withSuccess("""
                        {"id":"r1","name":"Trứng chiên cà chua","slug":"trung-chien-ca-chua","imageUrl":null,
                         "ingredients":[{"normalizedName":"trung ga","essential":true,"weight":1.0},
                                        {"normalizedName":"ca chua","essential":true,"weight":1.0}]}
                        """, MediaType.APPLICATION_JSON));

        // r2 chứa "tom" -- allergenGroup "hai_san" -- nhưng searchByIngredients không nhận allergen
        // nào cả (Set.of()) nên KHÔNG bị loại, khác hẳn suggest() ở test phía trên.
        recipeServer.expect(requestTo("http://recipe/recipes/r2"))
                .andRespond(withSuccess("""
                        {"id":"r2","name":"Tôm rang me","slug":"tom-rang-me","imageUrl":null,
                         "ingredients":[{"normalizedName":"tom","essential":true,"weight":1.0}]}
                        """, MediaType.APPLICATION_JSON));

        List<RecipeMatchResponse> result = service.searchByIngredients(List.of("Trứng gà", "Tôm"), 5);

        assertEquals(2, result.size()); // cả 2 đều còn -- AllergyFilterRule no-op vì allergen set rỗng
        // r2 (R={tom}, khớp trọn 1/1 nguyên liệu) xếp trên r1 (R={trung ga,ca chua}, chỉ khớp 1/2)
        assertEquals("r2", result.get(0).recipeId());
        assertEquals("r1", result.get(1).recipeId());

        recipeServer.verify();
    }

    @Test
    void availabilityComparesQuantityAgainstInventory() {
        MatchingService service = buildService();

        recipeServer.expect(requestTo("http://recipe/recipes/r1"))
                .andRespond(withSuccess("""
                        {"id":"r1","name":"Món test","slug":"mon-test","imageUrl":null,
                         "ingredients":[
                           {"name":"Trứng gà","normalizedName":"trung ga","amount":300,"unit":"g","essential":true,"weight":1.0},
                           {"name":"Cà chua","normalizedName":"ca chua","amount":500,"unit":"g","essential":true,"weight":1.0},
                           {"name":"Muối","normalizedName":"muoi","amount":10,"unit":"g","essential":false,"weight":0.3},
                           {"name":"Hành lá","normalizedName":"hanh la","amount":1,"unit":"nhánh","essential":false,"weight":0.3}
                         ]}
                        """, MediaType.APPLICATION_JSON));

        inventoryServer.expect(requestTo("http://inventory/inventory/items"))
                .andExpect(header("Authorization", BEARER))
                .andRespond(withSuccess("""
                        [{"id":1,"normalizedName":"trung ga","quantity":200,"unit":"g","status":"FRESH"},
                         {"id":2,"normalizedName":"muoi","quantity":50,"unit":"g","status":"FRESH"},
                         {"id":3,"normalizedName":"hanh la","quantity":2,"unit":"nhánh","status":"FRESH"}]
                        """, MediaType.APPLICATION_JSON));

        RecipeAvailabilityResponse result = service.checkAvailability("r1", BEARER);

        assertEquals("r1", result.recipeId());
        Map<String, IngredientAvailabilityResponse> byName = result.ingredients().stream()
                .collect(java.util.stream.Collectors.toMap(IngredientAvailabilityResponse::normalizedName, i -> i));

        assertEquals("PARTIAL", byName.get("trung ga").status()); // cần 300g, có 200g
        assertEquals(100.0, byName.get("trung ga").shortfallAmount(), 1e-9);

        assertEquals("MISSING", byName.get("ca chua").status()); // không có trong tủ lạnh
        assertEquals(500.0, byName.get("ca chua").shortfallAmount(), 1e-9);

        assertEquals("SUFFICIENT", byName.get("muoi").status()); // cần 10g, có 50g
        assertEquals(0, byName.get("hanh la").availableGrams(), 1e-9); // "nhánh" không có unitToGram -> gram=0
        // catalog fixture (IngredientEntry.basic) không có unitToGram cho "nhánh" -> không quy đổi được lượng cần,
        // nhưng lô hành lá vẫn tồn tại trong tủ lạnh -> UNKNOWN (khác MISSING)
        assertEquals("UNKNOWN", byName.get("hanh la").status());

        recipeServer.verify();
        inventoryServer.verify();
    }
}
