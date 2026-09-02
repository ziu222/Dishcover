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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chính sách xác thực của tủ lạnh ảo, chạy qua toàn bộ filter chain thật.
 *
 * <p>Trước đây nhóm endpoint này còn bị chặn theo gói cước ({@code @RequiresPlan("PRO")}). Mô hình
 * Freemium đã gỡ khỏi phạm vi đề tài cùng Payment Service (2026-08-17), nên yêu cầu còn lại chỉ là
 * JWT hợp lệ — vẫn phải kiểm, vì đây là dữ liệu riêng của từng người dùng.</p>
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

    private String token() {
        return "Bearer " + new JwtService(SECRET, 120).issue(1L, "chef@test.com", "FREE");
    }

    @Test
    void noTokenReturns401() throws Exception {
        mvc.perform(get("/inventory/items")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanList() throws Exception {
        when(service.list(any(), any())).thenReturn(List.of());

        mvc.perform(get("/inventory/items").header("Authorization", token()))
                .andExpect(status().isOk());
    }

    /** Endpoint ghi cũng phải chặn khi thiếu token, không chỉ endpoint đọc. */
    @Test
    void addItemWithoutTokenReturns401() throws Exception {
        mvc.perform(post("/inventory/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientName\":\"Trứng gà\",\"quantity\":2,\"unit\":\"quả\"}"))
                .andExpect(status().isUnauthorized());
    }

    /** Thêm theo lô là đường đi của luồng nhận diện ảnh (CLAUDE.md mục 7) — cũng cần token. */
    @Test
    void addBatchWithoutTokenReturns401() throws Exception {
        mvc.perform(post("/inventory/items/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"ingredientName\":\"Cà chua\",\"quantity\":1,\"unit\":\"quả\"}]}"))
                .andExpect(status().isUnauthorized());
    }

    /** Trừ kho sau khi nấu cũng là endpoint ghi -- cần token như add/addBatch. */
    @Test
    void cookDeductWithoutTokenReturns401() throws Exception {
        mvc.perform(post("/inventory/items/cook-deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"normalizedName\":\"trung ga\",\"amount\":2,\"unit\":\"quả\"}]}"))
                .andExpect(status().isUnauthorized());
    }

    /** Danh sách gợi ý tên nguyên liệu (autocomplete) cũng chỉ yêu cầu JWT hợp lệ, không public. */
    @Test
    void catalogIngredientsWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/inventory/catalog/ingredients")).andExpect(status().isUnauthorized());
    }

    @Test
    void catalogIngredientsWithTokenReturns200() throws Exception {
        mvc.perform(get("/inventory/catalog/ingredients").header("Authorization", token()))
                .andExpect(status().isOk());
    }

    // --- Regression: validation ở biên + lỗi KHÔNG được biến thành 401 (HIGH-2 + BUG errors→401) ---

    /** Tên vượt cột VARCHAR(100) phải bị chặn ở biên (422), không lọt xuống DB. */
    @Test
    void oversizedNameReturns422() throws Exception {
        String name = "X".repeat(150);
        mvc.perform(post("/inventory/items").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientName\":\"" + name + "\",\"quantity\":1,\"unit\":\"g\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    /** Đơn vị vượt cột VARCHAR(20) — trước đây không có ràng buộc nào, lọt xuống DB rồi lỗi. */
    @Test
    void oversizedUnitReturns422() throws Exception {
        String unit = "u".repeat(50);
        mvc.perform(post("/inventory/items").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientName\":\"Muối\",\"quantity\":1,\"unit\":\"" + unit + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Regression cho bug "mọi lỗi → 401": request ĐÃ xác thực nhưng body sai (ngày không parse
     * được) PHẢI trả 400, KHÔNG được trả 401 (trước đây bị che thành 401 do ERROR dispatch).
     */
    @Test
    void malformedBodyReturns400NotUnauthorized() throws Exception {
        mvc.perform(post("/inventory/items").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientName\":\"Cà chua\",\"quantity\":1,\"unit\":\"g\",\"expiryDate\":\"31-12-2026\"}"))
                .andExpect(status().isBadRequest());
    }
}
