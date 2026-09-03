package com.dishcover.rag.controller;

import com.dishcover.rag.dto.EmbedDtos.EmbedRequest;
import com.dishcover.rag.dto.EmbedDtos.EmbedResponse;
import com.dishcover.rag.embedding.EmbeddingGateway;
import com.dishcover.rag.exception.ApiExceptions.UpstreamUnavailableException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nội bộ — Recipe Service gọi lúc index công thức (tạo/sửa, CLAUDE.md mục 6 "Kích hoạt"). Vẫn yêu
 * cầu JWT hợp lệ như mọi endpoint {@code /internal/*} khác trong hệ thống (route Gateway là prefix
 * match phẳng, ai cũng gọi thẳng vào được — cùng lý do {@code MatchingController.matchByIngredients}).
 */
@RestController
public class EmbedController {

    private final EmbeddingGateway embeddingGateway;

    public EmbedController(EmbeddingGateway embeddingGateway) {
        this.embeddingGateway = embeddingGateway;
    }

    /**
     * @param request văn bản cần embed
     * @return vector 768 chiều
     * @throws UpstreamUnavailableException nếu embedding provider lỗi/timeout — Recipe Service (bên
     *         gọi) coi 503 này là fail-open, không chặn việc lưu công thức
     */
    @PostMapping("/internal/embed")
    public EmbedResponse embed(@Valid @RequestBody EmbedRequest request) {
        return embeddingGateway.embed(request.text())
                .map(EmbedResponse::new)
                .orElseThrow(() -> new UpstreamUnavailableException(
                        "Embedding provider tạm thời không khả dụng"));
    }
}
