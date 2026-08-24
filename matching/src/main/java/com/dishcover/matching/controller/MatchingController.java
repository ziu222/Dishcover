package com.dishcover.matching.controller;

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
     * Gợi ý công thức theo nguyên liệu người dùng đang có trong tủ lạnh ảo.
     *
     * @param bearerToken header Authorization ("Bearer &lt;token&gt;") của người dùng đang gọi
     * @param topN số lượng kết quả tối đa mong muốn; có thể null
     * @return danh sách công thức phù hợp, sắp xếp giảm dần theo điểm số
     */
    @GetMapping("/suggestions")
    public List<RecipeMatchResponse> suggestions(@RequestHeader("Authorization") String bearerToken,
                                                  @RequestParam(required = false) Integer topN) {
        return service.suggest(bearerToken, topN);
    }

    /**
     * Nội bộ — dùng cho RAG Service (specs/rag-service.md mục 1.1). Vẫn yêu cầu JWT hợp lệ vì
     * route Gateway là prefix match phẳng, ai cũng gọi thẳng vào được chứ không riêng RAG Service;
     * chỉ bỏ phần chặn theo gói cước sau khi gỡ Freemium.
     *
     * @param request danh sách nguyên liệu cần so khớp và số lượng kết quả mong muốn
     * @return danh sách công thức phù hợp, sắp xếp giảm dần theo điểm số
     */
    @PostMapping("/internal/match-by-ingredients")
    public List<RecipeMatchResponse> matchByIngredients(@Valid @RequestBody MatchByIngredientsRequest request) {
        return service.searchByIngredients(request.ingredients(), request.topN());
    }
}
