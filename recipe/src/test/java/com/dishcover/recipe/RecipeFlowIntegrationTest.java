package com.dishcover.recipe;

import com.dishcover.common.security.JwtService;
import com.dishcover.recipe.repository.RecipeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Chạy trên MongoDB thật, database recipe_matcher_test_db riêng (specs/recipe-service.md mục 6).
 * Mỗi test tự dọn recipe đã tạo ở @AfterEach — không giả định database rỗng.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeFlowIntegrationTest {

    private static final String SECRET = "test-secret-at-least-32-chars-long-000";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    RecipeRepository repo;

    private final List<String> createdIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        createdIds.forEach(repo::deleteById);
        createdIds.clear();
    }

    private String auth() {
        return "Bearer " + new JwtService(SECRET, 120).issue(1L, "chef@test.com", "FREE");
    }

    private String createPayload(String name, String tag) {
        return """
                {"name":"%s","cookTimeMinutes":15,"difficulty":"EASY","tags":["%s"],
                 "ingredients":[
                   {"name":"Trứng gà","amount":2,"unit":"quả","essential":true},
                   {"name":"Hành lá","amount":1,"unit":"nhánh","essential":false}
                 ],
                 "steps":[{"order":1,"title":"Sơ chế","content":"Rửa sạch","durationMinutes":5}]}
                """.formatted(name, tag);
    }

    private String create(String name, String tag) throws Exception {
        String body = mvc.perform(post("/recipes")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(name, tag)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(body).get("id").asText();
        createdIds.add(id);
        return id;
    }

    @Test
    void listWithoutTokenReturns200() throws Exception {
        mvc.perform(get("/recipes")).andExpect(status().isOk());
    }

    /** size quá lớn phải bị chặn ở max-page-size (100) để tránh tải toàn bộ collection. */
    @Test
    void oversizedPageSizeIsCappedAt100() throws Exception {
        mvc.perform(get("/recipes").param("size", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageSize").value(100));
    }

    @Test
    void createWithoutTokenReturns401() throws Exception {
        mvc.perform(post("/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload("Món test", "tag-" + System.nanoTime())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createComputesWeightAndNormalizedNameFromEssentialFlag() throws Exception {
        String tag = "test-" + System.nanoTime();
        String body = mvc.perform(post("/recipes")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload("Trứng chiên test " + tag, tag)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredients[0].essential").value(true))
                .andExpect(jsonPath("$.ingredients[0].weight").value(1.0))
                .andExpect(jsonPath("$.ingredients[0].normalizedName").value("trung ga"))
                .andExpect(jsonPath("$.ingredients[1].essential").value(false))
                .andExpect(jsonPath("$.ingredients[1].weight").value(0.3))
                .andExpect(jsonPath("$.ingredients[1].normalizedName").value("hanh la"))
                .andExpect(jsonPath("$.slug").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        createdIds.add(mapper.readTree(body).get("id").asText());
    }

    @Test
    void getByIdReturnsFullDetailIncludingSteps() throws Exception {
        String tag = "test-" + System.nanoTime();
        String id = create("Món chi tiết " + tag, tag);

        mvc.perform(get("/recipes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[0].title").value("Sơ chế"))
                .andExpect(jsonPath("$.ingredients.length()").value(2));
    }

    @Test
    void updateAppliesOnlyProvidedFields() throws Exception {
        String tag = "test-" + System.nanoTime();
        String id = create("Món sửa " + tag, tag);

        mvc.perform(patch("/recipes/" + id)
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cookTimeMinutes\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cookTimeMinutes").value(30))
                .andExpect(jsonPath("$.difficulty").value("EASY")); // không gửi -> giữ nguyên
    }

    @Test
    void updateWithoutTokenReturns401() throws Exception {
        String tag = "test-" + System.nanoTime();
        String id = create("Món sửa 401 " + tag, tag);

        mvc.perform(patch("/recipes/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cookTimeMinutes\":30}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteRemovesRecipe() throws Exception {
        String tag = "test-" + System.nanoTime();
        String id = create("Món xóa " + tag, tag);

        mvc.perform(delete("/recipes/" + id).header("Authorization", auth()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/recipes/" + id)).andExpect(status().isNotFound());
        createdIds.remove(id); // đã xóa, không cần cleanup lại
    }

    @Test
    void deleteWithoutTokenReturns401() throws Exception {
        String tag = "test-" + System.nanoTime();
        String id = create("Món xóa 401 " + tag, tag);

        mvc.perform(delete("/recipes/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void listFiltersByTagAndSupportsPagination() throws Exception {
        String tag = "test-filter-" + System.nanoTime();
        create("Món lọc A " + tag, tag);
        create("Món lọc B " + tag, tag);

        mvc.perform(get("/recipes").param("tag", tag).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].ingredients").doesNotExist()); // summary, không kèm ingredients
    }

    @Test
    void createRejectsEmptyIngredientsAndSteps() throws Exception {
        mvc.perform(post("/recipes")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rỗng\",\"cookTimeMinutes\":5,\"difficulty\":\"EASY\","
                                + "\"ingredients\":[],\"steps\":[]}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
