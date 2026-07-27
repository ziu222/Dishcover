package com.dishcover.rag.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class ChatDtos {

    private ChatDtos() {
    }

    /** conversationId nullable — client tự sinh UUID, thiếu thì bỏ qua đọc/ghi lịch sử (mục 3.8). */
    public record ChatRequest(@NotBlank String message, String conversationId) {
    }

    public record ChatResponse(String answer, List<String> sourceRecipeIds, boolean fallback) {
    }
}
