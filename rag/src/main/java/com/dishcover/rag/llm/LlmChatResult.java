package com.dishcover.rag.llm;

/**
 * Kết quả gọi {@link LlmGateway#chat}. {@code usedFallback=true} khi lời gọi LLM lỗi/timeout
 * (circuit breaker mở, exception...) HOẶC khi LLM trả về nội dung null/rỗng (VD bị Gemini safety
 * filter chặn) — cả hai trường hợp đều KHÔNG coi là câu trả lời hợp lệ, {@code answer} khi đó
 * là {@code null} và caller ({@code ChatOrchestrator}) phải tự dựng câu trả lời fallback từ danh
 * sách công thức thô.
 *
 * @param answer      nội dung câu trả lời của LLM, {@code null} nếu {@code usedFallback=true}
 * @param usedFallback true nếu phải dùng đường fallback thay vì câu trả lời LLM thật
 */
public record LlmChatResult(String answer, boolean usedFallback) {
}
