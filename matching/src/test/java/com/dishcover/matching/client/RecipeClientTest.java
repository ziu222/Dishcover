package com.dishcover.matching.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Bug thật phát hiện lúc live-verify Stage 8 (2026-09-07, docs/specs/diet-direction-recommendation.md):
 * RecipeClient gửi size=500 nhưng Recipe Service tự kẹp về max-page-size=100
 * (recipe/application.yml) — client trước đây chỉ lấy ĐÚNG 1 trang, bỏ sót mọi công thức ở trang
 * sau khi tổng công thức > 100 (153 công thức thật lúc phát hiện, mất 53 cái — gần hết batch
 * Spoonacular Stage 7, seed SAU CÙNG nên rơi vào trang bị bỏ sót). Ảnh hưởng TOÀN BỘ
 * /matching/suggestions, không riêng gì TagPreferenceRule.
 */
class RecipeClientTest {

    @Test
    void getAllRecipesWithIngredientsFetchesEveryPageNotJustTheFirst() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RecipeClient client = new RecipeClient(builder, "http://recipe");

        server.expect(requestTo("http://recipe/recipes?size=500"))
                .andRespond(withSuccess("""
                        {"content":[{"id":"r1"},{"id":"r2"}],"last":false}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://recipe/recipes?size=500&page=1"))
                .andRespond(withSuccess("""
                        {"content":[{"id":"r3"}],"last":true}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://recipe/recipes/r1"))
                .andRespond(withSuccess("""
                        {"id":"r1","name":"r1","slug":"r1","imageUrl":null,"ingredients":[]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://recipe/recipes/r2"))
                .andRespond(withSuccess("""
                        {"id":"r2","name":"r2","slug":"r2","imageUrl":null,"ingredients":[]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://recipe/recipes/r3"))
                .andRespond(withSuccess("""
                        {"id":"r3","name":"r3","slug":"r3","imageUrl":null,"ingredients":[]}
                        """, MediaType.APPLICATION_JSON));

        List<RecipeDetailDto> result = client.getAllRecipesWithIngredients();

        assertEquals(3, result.size());
        assertEquals(List.of("r1", "r2", "r3"), result.stream().map(RecipeDetailDto::id).toList());
        server.verify();
    }

    @Test
    void singlePageResultDoesNotRequestASecondPage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RecipeClient client = new RecipeClient(builder, "http://recipe");

        server.expect(requestTo("http://recipe/recipes?size=500"))
                .andRespond(withSuccess("""
                        {"content":[{"id":"r1"}],"last":true}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://recipe/recipes/r1"))
                .andRespond(withSuccess("""
                        {"id":"r1","name":"r1","slug":"r1","imageUrl":null,"ingredients":[]}
                        """, MediaType.APPLICATION_JSON));

        List<RecipeDetailDto> result = client.getAllRecipesWithIngredients();

        assertEquals(1, result.size());
        server.verify(); // không có expect nào cho page=1 -> nếu client lỡ gọi thêm sẽ tự fail
    }
}
