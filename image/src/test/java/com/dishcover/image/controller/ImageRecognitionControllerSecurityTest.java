package com.dishcover.image.controller;

import com.dishcover.common.security.JwtService;
import com.dishcover.image.dto.ImageDtos.RecognizeResponse;
import com.dishcover.image.service.RecognitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chính sách xác thực /recognize, chạy qua toàn bộ filter chain thật — RecognitionService mock để
 * không gọi Vision API thật. Khác Recipe: Image Service KHÔNG có endpoint đọc công khai, mọi
 * request đều cần JWT (CLAUDE.md mục 8).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImageRecognitionControllerSecurityTest {

    private static final String SECRET = "test-secret-at-least-32-chars-long-000";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    RecognitionService service;

    private String token() {
        return "Bearer " + new JwtService(SECRET, 120).issue(1L, "chef@test.com", "FREE");
    }

    private MockMultipartFile pngFile() {
        return new MockMultipartFile("file", "fridge.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void noTokenReturns401() throws Exception {
        mvc.perform(multipart("/recognize").file(pngFile()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserReturns200() throws Exception {
        when(service.recognize(any(), any())).thenReturn(new RecognizeResponse(List.of()));
        mvc.perform(multipart("/recognize").file(pngFile()).header("Authorization", token()))
                .andExpect(status().isOk());
    }
}
