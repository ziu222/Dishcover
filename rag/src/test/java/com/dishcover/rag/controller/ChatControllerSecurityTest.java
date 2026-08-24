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

/**
 * Chính sách xác thực của /chat qua toàn bộ filter chain thật — ChatOrchestrator mock
 * (specs/rag-service.md mục 6). Freemium đã gỡ (2026-08-17) nên chỉ còn yêu cầu JWT hợp lệ.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerSecurityTest {

    private static final String SECRET = "test-secret-at-least-32-chars-long-000";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    ChatOrchestrator orchestrator;

    private String token() {
        return "Bearer " + new JwtService(SECRET, 120).issue(1L, "chef@test.com", "FREE");
    }

    @Test
    void noTokenReturns401() throws Exception {
        mvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"tôi có trứng\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserReturns200() throws Exception {
        when(orchestrator.handle(any(), any())).thenReturn(new ChatResponse("ok", List.of(), false));
        mvc.perform(post("/chat")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"tôi có trứng\"}"))
                .andExpect(status().isOk());
    }
}
