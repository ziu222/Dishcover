package com.dishcover.inventory;

import com.dishcover.common.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Inventory Service không tự phát hành JWT (User Service làm việc đó) — test tự ký token bằng
 * cùng JwtService/secret khớp application-test.yml để mô phỏng token thật từ User Service.
 *
 * <p>Token ký với plan {@code PRO} vì tủ lạnh ảo là tính năng trả phí (CLAUDE.md mục 8) — mọi
 * endpoint đều {@code @RequiresPlan("PRO")}. Phần kiểm tra chính sách gói cước (401/402/200)
 * nằm ở {@code InventoryControllerSecurityTest}; test này chỉ lo luồng nghiệp vụ.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryFlowIntegrationTest {

    private static final String SECRET = "test-secret-at-least-32-chars-long-000";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    private String tokenFor(long userId) {
        return new JwtService(SECRET, 120).issue(userId, "user" + userId + "@test.com", "PRO");
    }

    private String auth(long userId) {
        return "Bearer " + tokenFor(userId);
    }

    @Test
    void noTokenReturns401() throws Exception {
        mvc.perform(get("/inventory/items")).andExpect(status().isUnauthorized());
    }

    @Test
    void addItemNormalizesNameAndComputesDefaultExpiry() throws Exception {
        long uid = System.nanoTime();
        String body = mvc.perform(post("/inventory/items")
                        .header("Authorization", auth(uid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientName\":\"Cà chua bi\",\"quantity\":3,\"unit\":\"quả\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.normalizedName").value("ca chua"))
                .andExpect(jsonPath("$.status").value("FRESH"))
                .andExpect(jsonPath("$.expiryDate").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        // Cà chua có shelfLifeDays=7 trong catalog -> expiryDate phải = hôm nay + 7, không phải null/hôm nay
        var node = mapper.readTree(body);
        java.time.LocalDate expiry = java.time.LocalDate.parse(node.get("expiryDate").asText());
        org.junit.jupiter.api.Assertions.assertEquals(java.time.LocalDate.now().plusDays(7), expiry);
    }

    @Test
    void sameBatchAccumulatesQuantityDifferentExpiryCreatesNewRow() throws Exception {
        long uid = System.nanoTime();
        java.time.LocalDate expiry = java.time.LocalDate.now().plusDays(10);

        addRaw(uid, "Trứng gà", "3", expiry.toString());
        addRaw(uid, "Trứng gà", "2", expiry.toString()); // cùng lô -> cộng dồn thành 5
        addRaw(uid, "Trứng gà", "1", expiry.plusDays(5).toString()); // lô khác -> dòng mới

        mvc.perform(get("/inventory/items").header("Authorization", auth(uid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // 2 lô, không phải 3 dòng cộng dồn sai
                .andExpect(jsonPath("$[?(@.expiryDate=='" + expiry + "')].quantity").value(5.0));
    }

    private void addRaw(long uid, String name, String qty, String expiry) throws Exception {
        mvc.perform(post("/inventory/items")
                        .header("Authorization", auth(uid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientName\":\"" + name + "\",\"quantity\":" + qty
                                + ",\"expiryDate\":\"" + expiry + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getUpdateDeleteScopedToOwner() throws Exception {
        long owner = System.nanoTime();
        long stranger = owner + 1;

        String created = mvc.perform(post("/inventory/items")
                        .header("Authorization", auth(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientName\":\"Tỏi\",\"quantity\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();

        // Stranger không thấy được item của owner -> 404 (không phải 403, tránh lộ tồn tại)
        mvc.perform(get("/inventory/items/" + id).header("Authorization", auth(stranger)))
                .andExpect(status().isNotFound());

        // Owner update quantity + đánh dấu USED
        mvc.perform(patch("/inventory/items/" + id)
                        .header("Authorization", auth(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"USED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        // Stranger không xóa được item của owner -> 404 (nhất quán với GET/PATCH, không còn 204 giả)
        mvc.perform(delete("/inventory/items/" + id).header("Authorization", auth(stranger)))
                .andExpect(status().isNotFound());

        // Item của owner vẫn còn nguyên (chứng minh xóa "hụt" ở trên không ảnh hưởng ownership)
        mvc.perform(get("/inventory/items/" + id).header("Authorization", auth(owner)))
                .andExpect(status().isOk());

        // Owner xóa thật -> 204, và sau đó không còn (happy path + xóa lần nữa -> 404)
        mvc.perform(delete("/inventory/items/" + id).header("Authorization", auth(owner)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/inventory/items/" + id).header("Authorization", auth(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void expiredItemDerivesStatusWithoutCron() throws Exception {
        long uid = System.nanoTime();
        String created = mvc.perform(post("/inventory/items")
                        .header("Authorization", auth(uid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientName\":\"Sữa tươi\",\"quantity\":1,"
                                + "\"expiryDate\":\"" + java.time.LocalDate.now().minusDays(1) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXPIRED"))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();

        // Đọc lại vẫn EXPIRED dù DB lưu gì — derived tại thời điểm đọc
        mvc.perform(get("/inventory/items/" + id).header("Authorization", auth(uid)))
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    void negativeQuantityFailsValidation() throws Exception {
        long uid = System.nanoTime();
        mvc.perform(post("/inventory/items")
                        .header("Authorization", auth(uid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientName\":\"Hành lá\",\"quantity\":-1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void batchAddWorksForImageRecognitionFlow() throws Exception {
        long uid = System.nanoTime();
        mvc.perform(post("/inventory/items/batch")
                        .header("Authorization", auth(uid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"ingredientName\":\"Cà rốt\",\"quantity\":2},"
                                + "{\"ingredientName\":\"Khoai tây\",\"quantity\":4}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].normalizedName").value("ca rot"))
                .andExpect(jsonPath("$[1].normalizedName").value("khoai tay"));
    }
}
