package com.dishcover.inventory.controller;

import com.dishcover.common.security.JwtService;
import com.dishcover.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tủ lạnh ảo là tính năng gói PRO (CLAUDE.md mục 8) — test {@code @RequiresPlan} chạy qua toàn bộ
 * filter chain thật (JWT verify + AOP), {@code InventoryService} mock để không đụng DB.
 *
 * <p>Trước đây Inventory KHÔNG gate gói cước (chỉ {@code .anyRequest().authenticated()}), nên user
 * FREE dùng được tính năng trả phí — phát hiện lúc đối chiếu sơ đồ use case với code thật.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerSecurityTest {

    private static final String SECRET = "test-secret-at-least-32-chars-long-000";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    InventoryService service;

    private String token(String plan) {
        return "Bearer " + new JwtService(SECRET, 120).issue(1L, "chef@test.com", plan);
    }

    @Test
    void noTokenReturns401() throws Exception {
        mvc.perform(get("/inventory/items")).andExpect(status().isUnauthorized());
    }

    @Test
    void freePlanReturns402PaymentRequired() throws Exception {
        mvc.perform(get("/inventory/items").header("Authorization", token("FREE")))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PAYMENT_REQUIRED"));
    }

    @Test
    void proPlanReturns200() throws Exception {
        when(service.list(any(), any())).thenReturn(List.of());
        mvc.perform(get("/inventory/items").header("Authorization", token("PRO")))
                .andExpect(status().isOk());
    }

    /** Endpoint ghi cũng phải bị chặn — không chỉ endpoint đọc. */
    @Test
    void freePlanCannotAddItem() throws Exception {
        mvc.perform(post("/inventory/items")
                        .header("Authorization", token("FREE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientName\":\"Trứng gà\",\"quantity\":2,\"unit\":\"quả\"}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PAYMENT_REQUIRED"));
    }

    /** Thêm theo lô là đường đi của luồng nhận diện ảnh (CLAUDE.md mục 7) — cũng phải PRO. */
    @Test
    void freePlanCannotAddBatch() throws Exception {
        mvc.perform(post("/inventory/items/batch")
                        .header("Authorization", token("FREE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"ingredientName\":\"Cà chua\",\"quantity\":1,\"unit\":\"quả\"}]}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PAYMENT_REQUIRED"));
    }
}
