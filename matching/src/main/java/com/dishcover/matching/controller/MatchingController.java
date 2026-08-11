package com.dishcover.matching.controller;

import com.dishcover.common.security.RequiresPlan;
import com.dishcover.matching.dto.MatchingDtos.MatchByIngredientsRequest;
import com.dishcover.matching.dto.MatchingDtos.RecipeMatchResponse;
import com.dishcover.matching.service.MatchingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gợi ý công thức theo nguyên liệu — tính năng PRO (CLAUDE.md mục 8). */
@RestController
@RequestMapping("/matching")
public class MatchingController {

    private final MatchingService service;

    /**
     * @param service service orchestration chấm điểm/gợi ý công thức
     */
    public MatchingController(MatchingService service) {
        this.service = service;
    }

    /**
     * Gợi ý công thức theo nguyên liệu người dùng đang đăng nhập đang có trong tủ lạnh — tính năng PRO.
     *
     * @param bearerToken header Authorization ("Bearer &lt;token&gt;") của người dùng đang gọi
     * @param topN số lượng kết quả tối đa mong muốn; có thể null
     * @return danh sách công thức phù hợp, sắp xếp giảm dần theo điểm số
     */
    @RequiresPlan("PRO")
    @GetMapping("/suggestions")
    public List<RecipeMatchResponse> suggestions(@RequestHeader("Authorization") String bearerToken,
                                                  @RequestParam(required = false) Integer topN) {
        return service.suggest(bearerToken, topN);
    }

    /**
     * Nội bộ — dùng cho RAG Service (specs/rag-service.md mục 1.1). VẪN giữ @RequiresPlan("PRO"):
     * Gateway route /matching-service/** là prefix match phẳng, bỏ gate sẽ lộ tính năng PRO miễn
     * phí cho bất kỳ ai gọi thẳng qua Gateway (specs/rag-service.md mục 1.2).
     *
     * @param request tên nguyên liệu tự do (VD trích từ câu hỏi chat) + topN mong muốn
     * @return danh sách công thức phù hợp, sắp xếp giảm dần theo điểm số
     */
    @RequiresPlan("PRO")
    @PostMapping("/internal/match-by-ingredients")
    public List<RecipeMatchResponse> matchByIngredients(@Valid @RequestBody MatchByIngredientsRequest request) {
        return service.searchByIngredients(request.ingredients(), request.topN());
    }
}
