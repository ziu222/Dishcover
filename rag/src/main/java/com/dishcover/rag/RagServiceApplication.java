package com.dishcover.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động của RAG Chatbot Service (CLAUDE.md mục 6) — service stateless gọi Matching
 * Service (lọc cứng nguyên liệu) và LLM ngoài (Gemini) để trả lời câu hỏi nấu ăn dựa trên công
 * thức thật trong hệ thống, giai đoạn A của kiến trúc Hybrid Retrieval.
 */
@SpringBootApplication
public class RagServiceApplication {

    /**
     * Khởi chạy Spring Boot application context của RAG Service.
     *
     * @param args tham số dòng lệnh, chuyển thẳng cho {@link SpringApplication#run}
     */
    public static void main(String[] args) {
        SpringApplication.run(RagServiceApplication.class, args);
    }
}
