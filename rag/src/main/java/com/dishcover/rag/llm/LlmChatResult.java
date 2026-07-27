package com.dishcover.rag.llm;

public record LlmChatResult(String answer, boolean usedFallback) {
}
