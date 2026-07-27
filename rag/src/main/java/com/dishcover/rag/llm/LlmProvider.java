package com.dishcover.rag.llm;

/**
 * Abstraction cho nhà cung cấp LLM (CLAUDE.md mục 9 "L"/"D") — nghiệp vụ (LlmGateway/
 * ChatOrchestrator) chỉ phụ thuộc interface này, KHÔNG import trực tiếp class Spring AI/Gemini cụ
 * thể. Chỉ {@link GeminiProvider} cần cho Definition of Done giai đoạn A — OllamaProvider (dự
 * phòng, CLAUDE.md mục 6) KHÔNG thuộc phạm vi, thêm sau chỉ là thêm 1 class mới implement
 * interface này, không đổi chữ ký.
 */
public interface LlmProvider {

    /** 1 chuỗi prompt vào, 1 chuỗi câu trả lời ra — template đã gộp sẵn history+context+question. */
    String complete(String prompt);
}
