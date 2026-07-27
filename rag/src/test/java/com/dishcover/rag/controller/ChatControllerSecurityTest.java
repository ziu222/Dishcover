package com.dishcover.rag.controller;

import com.dishcover.common.security.JwtService;
import com.dishcover.rag.dto.ChatDtos.ChatResponse;
import com.dishcover.rag.service.ChatOrchestrator;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test @RequiresPlan chạy qua toàn bộ filter chain thật — ChatOrchestrator mock (specs/rag-service.md mục 6). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerSecurityTest {

    private static final String SECRET = "test-secret-at-least-32-chars-long-000";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    ChatOrchestrator orchestrator;

    private String token(String plan) {
        return "Bearer " + new JwtService(SECRET, 120).issue(1L, "chef@test.com", plan);
    }

    @Test
    void noTokenReturns401() throws Exception {
        mvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"tôi có trứng\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void freePlanReturns402PaymentRequired() throws Exception {
        mvc.perform(post("/chat")
                        .header("Authorization", token("FREE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"tôi có trứng\"}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PAYMENT_REQUIRED"));
    }

    @Test
    void proPlanReturns200() throws Exception {
        when(orchestrator.handle(any(), any())).thenReturn(new ChatResponse("ok", List.of(), false));
        mvc.perform(post("/chat")
                        .header("Authorization", token("PRO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"tôi có trứng\"}"))
                .andExpect(status().isOk());
    }
}
