package com.dishcover.matching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;

/** DTO cho 2 endpoint nội bộ Giai đoạn B (RAG vector search) — CLAUDE.md mục 6. */
public final class IndexDtos {

    private IndexDtos() {
    }

    /**
     * Gọi từ Recipe Service sau khi lưu công thức (tạo/sửa) — RAG Service đã tính sẵn embedding,
     * Matching Service chỉ lưu (JDBC thuần vào bảng {@code recipe_embeddings} đã có sẵn từ trước,
     * không tự gọi AI nào — xem javadoc {@code IndexController}).
     *
     * @param recipeId id công thức bên Recipe Service (MongoDB) — soft-reference khác hệ DB
     * @param content  văn bản đại diện đã dùng để tính embedding (lưu lại để debug/tái tính sau)
     * @param embedding vector 768 chiều đã tính sẵn (RAG Service)
     * @param metadata  tùy chọn, thông tin phụ (VD tên món) — không dùng cho truy vấn, chỉ debug
     */
    public record IndexRequest(
            @NotBlank String recipeId,
            @NotBlank String content,
            @NotEmpty float[] embedding,
            Map<String, Object> metadata
    ) {
    }

    /**
     * @param embedding vector câu hỏi đã embed sẵn (RAG Service)
     * @param topK      số kết quả tối đa muốn lấy
     */
    public record VectorSearchRequest(
            @NotEmpty float[] embedding,
            @NotNull @Positive Integer topK
    ) {
    }

    /**
     * @param recipeId   id công thức khớp
     * @param similarity độ tương đồng cosine (1 = giống hệt, 0 = không liên quan, có thể âm)
     */
    public record VectorMatch(String recipeId, double similarity) {
    }

    /** @param matches danh sách công thức gần nhất, sắp xếp giảm dần theo similarity */
    public record VectorSearchResponse(List<VectorMatch> matches) {
    }
}
