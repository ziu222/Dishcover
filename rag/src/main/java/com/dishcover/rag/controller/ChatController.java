package com.dishcover.rag.controller;

import com.dishcover.common.security.RequestTokenExtractor;
import com.dishcover.rag.dto.ChatDtos.ChatRequest;
import com.dishcover.rag.dto.ChatDtos.ChatResponse;
import com.dishcover.rag.service.ChatOrchestrator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Chatbot RAG — chỉ yêu cầu JWT hợp lệ (Freemium đã gỡ, CLAUDE.md mục 8). Gateway: POST /rag-service/chat -> POST /chat. */
@RestController
public class ChatController {

    private final ChatOrchestrator orchestrator;

    public ChatController(ChatOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Nhận câu hỏi chat và trả lời dựa trên công thức thật trong hệ thống (giai đoạn A).
     * Chỉ yêu cầu JWT hợp lệ (mô hình Freemium đã gỡ 2026-08-17).
     *
     * <p>Token chuyển tiếp cho Matching/User lấy từ header Authorization HOẶC cookie
     * {@code auth_token} (trình duyệt dùng httpOnly cookie) — xem {@link RequestTokenExtractor}.</p>
     *
     * @param request nội dung câu hỏi + conversationId (nullable)
     * @return câu trả lời kèm {@code sourceRecipeIds} và cờ {@code fallback}
     */
    @PostMapping("/chat")
    public ChatResponse chat(HttpServletRequest httpRequest,
                              @Valid @RequestBody ChatRequest request) {
        return orchestrator.handle(RequestTokenExtractor.resolveBearer(httpRequest), request);
    }
}
