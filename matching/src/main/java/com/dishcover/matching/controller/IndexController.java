package com.dishcover.matching.controller;

import com.dishcover.matching.dto.IndexDtos.IndexRequest;
import com.dishcover.matching.dto.IndexDtos.VectorSearchRequest;
import com.dishcover.matching.dto.IndexDtos.VectorSearchResponse;
import com.dishcover.matching.service.RecipeIndexService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nội bộ — Giai đoạn B vector search (CLAUDE.md mục 6). Matching Service CHỈ lưu/tìm embedding đã
 * tính sẵn từ RAG Service (owns AI provider code) — không tự gọi embedding API nào, giữ đúng ranh
 * giới "Matching = business logic + DB, không gọi AI ngoài" đã có từ đầu dự án. Vẫn yêu cầu JWT hợp
 * lệ như mọi endpoint {@code /internal/*} khác (route Gateway là prefix match phẳng).
 *
 * <p><b>Luồng index</b>: Recipe Service (sau khi lưu MongoDB) tự dựng văn bản đại diện, gọi RAG
 * {@code POST /internal/embed} lấy vector, rồi gọi {@link #index} ở đây để lưu — best-effort, lỗi
 * ở bước nào cũng KHÔNG chặn việc lưu công thức (xem RecipeService phía Recipe Service).</p>
 *
 * <p><b>Luồng tìm</b>: RAG Service tự embed câu hỏi (trong tiến trình, không gọi lại chính mình),
 * rồi gọi {@link #vectorSearch} ở đây lấy top-K {@code recipeId} — kênh thứ 4 của
 * {@code HybridRetriever}, fail-open nếu lỗi (các kênh Giai đoạn A khác vẫn chạy bình thường).</p>
 */
@RestController
@RequestMapping("/matching")
public class IndexController {

    private final RecipeIndexService service;

    public IndexController(RecipeIndexService service) {
        this.service = service;
    }

    /**
     * @param req embedding công thức đã tính sẵn (RAG Service), kèm văn bản đại diện
     */
    @PostMapping("/internal/index")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void index(@Valid @RequestBody IndexRequest req) {
        service.index(req);
    }

    /**
     * @param req vector câu hỏi đã embed sẵn (RAG Service) + số kết quả mong muốn
     * @return top-K công thức gần nhất theo cosine similarity
     */
    @PostMapping("/internal/vector-search")
    public VectorSearchResponse vectorSearch(@Valid @RequestBody VectorSearchRequest req) {
        return new VectorSearchResponse(service.search(req));
    }
}
