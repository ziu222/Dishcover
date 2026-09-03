package com.dishcover.rag.embedding;

/**
 * Abstraction cho nhà cung cấp embedding (song song {@link com.dishcover.rag.llm.LlmProvider},
 * CLAUDE.md mục 9 "L"/"D") — nghiệp vụ (EmbeddingGateway) chỉ phụ thuộc interface này, không import
 * trực tiếp class Spring AI/OpenAI cụ thể. Giai đoạn B, chỉ {@link OpenAiEmbeddingProvider} cần cho
 * Definition of Done (Gemini/Groq không dùng ở đây — xem Javadoc {@link OpenAiEmbeddingProvider}).
 */
public interface EmbeddingProvider {

    /** 1 chuỗi văn bản vào, 1 vector embedding ra (768 chiều — khớp cột {@code vector(768)} Matching Service). */
    float[] embed(String text);
}
