package com.dishcover.matching.controller;

import com.dishcover.common.security.JwtService;
import com.dishcover.matching.service.MatchingService;
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
 * Chính sách xác thực của Matching Service, chạy qua toàn bộ filter chain thật — MatchingService
 * mock để không cần 3 service ngoài (specs/matching-service.md mục 6/7).
 *
 * <p>Trước đây còn kiểm gói cước; Freemium đã gỡ cùng Payment Service (2026-08-17) nên yêu cầu
 * còn lại chỉ là JWT hợp lệ.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatchingControllerSecurityTest {

    private static final String SECRET = "test-secret-at-least-32-chars-long-000";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    MatchingService service;

    private String token() {
        return "Bearer " + new JwtService(SECRET, 120).issue(1L, "chef@test.com", "FREE");
    }

    @Test
    void noTokenReturns401() throws Exception {
        mvc.perform(get("/matching/suggestions")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserReturns200() throws Exception {
        when(service.suggest(any(), any())).thenReturn(List.of());
        mvc.perform(get("/matching/suggestions").header("Authorization", token()))
                .andExpect(status().isOk());
    }

    /**
     * Regression: trình duyệt dùng httpOnly cookie {@code auth_token} (KHÔNG có header
     * Authorization) vẫn phải gọi được /suggestions — trước đây bắt buộc header nên frontend luôn 401.
     */
    @Test
    void acceptsAuthCookieWithoutHeader() throws Exception {
        when(service.suggest(any(), any())).thenReturn(List.of());
        String raw = new JwtService(SECRET, 120).issue(1L, "chef@test.com", "FREE");
        mvc.perform(get("/matching/suggestions")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", raw)))
                .andExpect(status().isOk());
    }

    /**
     * Endpoint nội bộ vẫn phải kiểm token: route Gateway là prefix match phẳng nên ai cũng gọi
     * thẳng vào được, không riêng RAG Service.
     */
    @Test
    void internalMatchByIngredientsNoTokenReturns401() throws Exception {
        mvc.perform(post("/matching/internal/match-by-ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredients\":[\"trứng gà\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalMatchByIngredientsAuthenticatedReturns200() throws Exception {
        when(service.searchByIngredients(any(), any())).thenReturn(List.of());
        mvc.perform(post("/matching/internal/match-by-ingredients")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredients\":[\"trứng gà\"]}"))
                .andExpect(status().isOk());
    }
}
