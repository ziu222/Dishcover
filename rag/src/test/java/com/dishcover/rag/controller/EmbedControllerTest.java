package com.dishcover.rag.controller;

import com.dishcover.common.security.JwtService;
import com.dishcover.rag.embedding.EmbeddingGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chính sách xác thực + fallback của /internal/embed qua toàn bộ filter chain thật — cùng pattern
 * {@code ChatControllerSecurityTest}. Gọi từ Recipe Service lúc index (CLAUDE.md mục 6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmbedControllerTest {

    private static final String SECRET = "test-secret-at-least-32-chars-long-000";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    EmbeddingGateway embeddingGateway;

    private String token() {
        return "Bearer " + new JwtService(SECRET, 120).issue(1L, "chef@test.com", "FREE");
    }

    @Test
    void noTokenReturns401() throws Exception {
        mvc.perform(post("/internal/embed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Trứng chiên cà chua\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedSuccessReturns200WithEmbedding() throws Exception {
        when(embeddingGateway.embed(anyString())).thenReturn(Optional.of(new float[]{0.1f, 0.2f}));
        mvc.perform(post("/internal/embed")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Trứng chiên cà chua\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embedding[0]").value(0.1f));
    }

    /** Fail-open ở tầng gọi (Recipe Service coi 503 là bỏ qua index, không chặn lưu công thức). */
    @Test
    void authenticatedProviderFailureReturns503() throws Exception {
        when(embeddingGateway.embed(anyString())).thenReturn(Optional.empty());
        mvc.perform(post("/internal/embed")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Trứng chiên cà chua\"}"))
                .andExpect(status().isServiceUnavailable());
    }
}
