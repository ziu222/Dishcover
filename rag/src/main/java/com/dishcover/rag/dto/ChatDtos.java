package com.dishcover.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Namespace các DTO request/response của {@code POST /chat}. */
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

    /**
     * Kết quả trả về cho client sau 1 lượt chat.
     *
     * @param answer          câu trả lời (LLM thật hoặc danh sách công thức thô nếu fallback)
     * @param sourceRecipeIds id các công thức thật đã dùng làm căn cứ trả lời, để truy vết
     * @param fallback        true nếu LLM lỗi/timeout và answer là fallback thô, không phải LLM
     * @param dietaryWarnings tên nguyên liệu vi phạm đặc điểm ăn uống đã khai báo, tính SẴN bằng
     *                        code (không phải trích từ văn bản LLM trả lời) — dùng để frontend
     *                        hiển thị badge/callout cảnh báo nổi bật, độc lập với cách LLM diễn
     *                        đạt trong {@code answer}. Rỗng nếu không có xung đột nào.
     */
    public record ChatResponse(String answer, List<String> sourceRecipeIds, boolean fallback,
                                List<String> dietaryWarnings) {
    }
}
