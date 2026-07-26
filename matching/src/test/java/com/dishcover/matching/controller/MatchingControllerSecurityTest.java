package com.dishcover.matching.controller;

import com.dishcover.common.security.JwtService;
import com.dishcover.matching.service.MatchingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test @RequiresPlan chạy qua toàn bộ filter chain thật (JWT verify + AOP) — MatchingService
 * được mock để không cần 3 service ngoài (specs/matching-service.md mục 6/7).
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

    private String token(String plan) {
        return "Bearer " + new JwtService(SECRET, 120).issue(1L, "chef@test.com", plan);
    }

    @Test
    void noTokenReturns401() throws Exception {
        mvc.perform(get("/matching/suggestions")).andExpect(status().isUnauthorized());
    }

    @Test
    void freePlanReturns402PaymentRequired() throws Exception {
        mvc.perform(get("/matching/suggestions").header("Authorization", token("FREE")))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PAYMENT_REQUIRED"));
    }

    @Test
    void proPlanReturns200() throws Exception {
        when(service.suggest(any(), any())).thenReturn(List.of());
        mvc.perform(get("/matching/suggestions").header("Authorization", token("PRO")))
                .andExpect(status().isOk());
    }
}
