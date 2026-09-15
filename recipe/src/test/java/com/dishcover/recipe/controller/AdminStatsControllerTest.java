package com.dishcover.recipe.controller;

import com.dishcover.common.security.JwtService;
import com.dishcover.recipe.repository.RecipeRepository;
import com.fasterxml.jackson.databind.JsonNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chạy trên MongoDB thật, database chung với {@code RecipeFlowIntegrationTest} — không giả định
 * collection rỗng nên so DELTA trước/sau khi tạo thay vì kỳ vọng con số tuyệt đối.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminStatsControllerTest {

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
        return "Bearer " + new JwtService(SECRET, 120).issue(1L, "chef@test.com", "FREE", "ADMIN");
    }

    private String authUser() {
        return "Bearer " + new JwtService(SECRET, 120).issue(2L, "user@test.com", "FREE", "USER");
    }

    private String create(String name, String difficulty) throws Exception {
        String tag = "stats-test-" + System.nanoTime();
        String payload = """
                {"name":"%s","cookTimeMinutes":15,"difficulty":"%s","tags":["%s"],
                 "ingredients":[{"name":"Trứng gà","amount":2,"unit":"quả","essential":true}],
                 "steps":[{"order":1,"title":"Sơ chế","content":"Rửa sạch","durationMinutes":5}]}
                """.formatted(name, difficulty, tag);
        String body = mvc.perform(post("/recipes")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(body).get("id").asText();
        createdIds.add(id);
        return id;
    }

    @Test
    void userThuongKhongXemDuocSoLieu() throws Exception {
        mvc.perform(get("/admin/stats").header("Authorization", authUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void khongCoTokenThiKhongVaoDuoc() throws Exception {
        mvc.perform(get("/admin/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    void tongVaTheoDoKhoTangDungBangSoCongThucVuaTao() throws Exception {
        JsonNode before = mapper.readTree(mvc.perform(get("/admin/stats").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        create("Món dễ 1 " + System.nanoTime(), "EASY");
        create("Món dễ 2 " + System.nanoTime(), "EASY");
        create("Món khó " + System.nanoTime(), "HARD");

        JsonNode after = mapper.readTree(mvc.perform(get("/admin/stats").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertThat(after.get("totalRecipes").asLong() - before.get("totalRecipes").asLong()).isEqualTo(3);
        assertThat(after.get("easyCount").asLong() - before.get("easyCount").asLong()).isEqualTo(2);
        assertThat(after.get("hardCount").asLong() - before.get("hardCount").asLong()).isEqualTo(1);
        assertThat(after.get("newRecipesLast7Days").asLong() - before.get("newRecipesLast7Days").asLong())
                .isEqualTo(3);
    }
}
