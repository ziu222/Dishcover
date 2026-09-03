package com.dishcover.matching.repository;

import com.dishcover.matching.dto.IndexDtos.VectorMatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Đọc/ghi trực tiếp bảng {@code matching_service.recipe_embeddings} (đã tồn tại sẵn từ Flyway V1,
 * CLAUDE.md mục 3.1 — chưa dùng tới trước Giai đoạn B). Dùng {@link JdbcTemplate} thuần thay vì
 * Spring Data JPA repository: cột {@code embedding vector(768)} của pgvector không có mapping JPA
 * chuẩn sẵn, thêm thư viện {@code pgvector-java} chỉ cho 2 câu lệnh này không đáng — build vector
 * literal bằng tay ({@code '[0.1,0.2,...]'::vector}, đúng cú pháp pgvector chấp nhận) đơn giản hơn.
 */
@Repository
public class RecipeEmbeddingRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public RecipeEmbeddingRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /**
     * Ghi/thay embedding của 1 công thức — xóa lô cũ (nếu có) rồi insert lô mới trong 1 transaction,
     * đơn giản hơn {@code ON CONFLICT} (bảng không có UNIQUE constraint trên {@code recipe_id}, chỉ
     * có index thường — không cần thêm migration chỉ để hỗ trợ upsert).
     *
     * @param recipeId    id công thức bên Recipe Service
     * @param content     văn bản đại diện đã dùng để tính embedding
     * @param embedding   vector đã tính sẵn
     * @param metadata    thông tin phụ tùy chọn, null nếu không có
     */
    @Transactional
    public void upsert(String recipeId, String content, float[] embedding, Map<String, Object> metadata) {
        jdbc.update("DELETE FROM recipe_embeddings WHERE recipe_id = ?", recipeId);
        String metadataJson = toJson(metadata);
        if (metadataJson == null) {
            jdbc.update(
                    "INSERT INTO recipe_embeddings (recipe_id, content, embedding) VALUES (?, ?, CAST(? AS vector))",
                    recipeId, content, toVectorLiteral(embedding));
        } else {
            jdbc.update(
                    "INSERT INTO recipe_embeddings (recipe_id, content, metadata, embedding) VALUES (?, ?, CAST(? AS jsonb), CAST(? AS vector))",
                    recipeId, content, metadataJson, toVectorLiteral(embedding));
        }
    }

    /**
     * Top-K công thức gần {@code queryEmbedding} nhất theo cosine distance ({@code <=>} toán tử
     * pgvector, đã có HNSW index sẵn — CLAUDE.md mục 3.1).
     *
     * @param queryEmbedding vector câu hỏi đã embed sẵn
     * @param topK           số kết quả tối đa
     * @return danh sách khớp, sắp giảm dần theo similarity (1 - cosine distance)
     */
    public List<VectorMatch> findNearest(float[] queryEmbedding, int topK) {
        String literal = toVectorLiteral(queryEmbedding);
        return jdbc.query(
                "SELECT recipe_id, 1 - (embedding <=> CAST(? AS vector)) AS similarity "
                        + "FROM recipe_embeddings ORDER BY embedding <=> CAST(? AS vector) LIMIT ?",
                (rs, rowNum) -> new VectorMatch(rs.getString("recipe_id"), rs.getDouble("similarity")),
                literal, literal, topK);
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception ex) {
            return null; // metadata chỉ để debug -- lỗi serialize không đáng chặn cả lần index
        }
    }

    private static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 8).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
