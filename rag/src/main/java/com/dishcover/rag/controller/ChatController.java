package com.dishcover.rag.controller;

import com.dishcover.common.security.RequiresPlan;
import com.dishcover.rag.dto.ChatDtos.ChatRequest;
import com.dishcover.rag.dto.ChatDtos.ChatResponse;
import com.dishcover.rag.service.ChatOrchestrator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Chatbot RAG — tính năng PRO (CLAUDE.md mục 8). Gateway: POST /rag-service/chat -> POST /chat. */
@RestController
public class ChatController {

    private final ChatOrchestrator orchestrator;

    public ChatController(ChatOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @RequiresPlan("PRO")
    @PostMapping("/chat")
    public ChatResponse chat(@RequestHeader("Authorization") String bearerToken,
                              @Valid @RequestBody ChatRequest request) {
        return orchestrator.handle(bearerToken, request);
    }
}
