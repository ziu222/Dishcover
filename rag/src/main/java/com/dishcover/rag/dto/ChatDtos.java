package com.dishcover.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ChatDtos {

    private ChatDtos() {
    }

    /**
     * conversationId nullable — client tự sinh UUID, thiếu thì bỏ qua đọc/ghi lịch sử (mục 3.8).
     * Giới hạn độ dài: chặn 1 client gửi message khổng lồ (tốn phí gọi LLM thật) hoặc
     * conversationId bất thường dùng làm key ConcurrentHashMap.
     */
    public record ChatRequest(
            @NotBlank @Size(max = 2000) String message,
            @Size(max = 64) String conversationId
    ) {
    }

    public record ChatResponse(String answer, List<String> sourceRecipeIds, boolean fallback) {
    }
}
