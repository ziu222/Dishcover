package com.dishcover.matching.service;

import com.dishcover.matching.dto.IndexDtos.IndexRequest;
import com.dishcover.matching.dto.IndexDtos.VectorMatch;
import com.dishcover.matching.dto.IndexDtos.VectorSearchRequest;
import com.dishcover.matching.repository.RecipeEmbeddingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Giai đoạn B (RAG vector search, CLAUDE.md mục 6) — Matching Service chỉ lưu/tìm embedding đã
 * tính sẵn (RAG Service), không tự gọi AI. Xem javadoc {@code RecipeEmbeddingRepository} vì sao
 * dùng JDBC thuần thay vì JPA entity.
 */
@Service
public class RecipeIndexService {

    private final RecipeEmbeddingRepository repo;

    public RecipeIndexService(RecipeEmbeddingRepository repo) {
        this.repo = repo;
    }

    /** @param req embedding công thức đã tính sẵn, kèm văn bản đại diện + metadata tùy chọn */
    public void index(IndexRequest req) {
        repo.upsert(req.recipeId(), req.content(), req.embedding(), req.metadata());
    }

    /** @param req vector câu hỏi đã embed sẵn + số kết quả mong muốn */
    public List<VectorMatch> search(VectorSearchRequest req) {
        return repo.findNearest(req.embedding(), req.topK());
    }
}
