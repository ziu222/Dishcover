package com.dishcover.rag.dto;

import jakarta.validation.constraints.NotBlank;

/** DTO cho POST /internal/embed — gọi từ Recipe Service (index) và Matching Service KHÔNG gọi endpoint này (RAG tự embed câu hỏi trong tiến trình, xem HybridRetriever). */
public final class EmbedDtos {

    private EmbedDtos() {
    }

    /** @param text văn bản đại diện cần embed (VD nội dung công thức lúc index) */
    public record EmbedRequest(@NotBlank String text) {
    }

    /** @param embedding vector 768 chiều */
    public record EmbedResponse(float[] embedding) {
    }
}
