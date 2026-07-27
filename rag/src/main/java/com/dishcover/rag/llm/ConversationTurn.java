package com.dishcover.rag.llm;

import java.time.Instant;

/** 1 lượt hội thoại — role: "user" | "assistant" (specs/rag-service.md mục 3.8). */
public record ConversationTurn(String role, String text, Instant at) {
}
