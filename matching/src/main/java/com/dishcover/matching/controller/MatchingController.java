package com.dishcover.matching.controller;

import com.dishcover.common.security.RequestTokenExtractor;
import com.dishcover.matching.dto.MatchingDtos.MatchByIngredientsRequest;
import com.dishcover.matching.dto.MatchingDtos.RecipeAvailabilityResponse;
import com.dishcover.matching.dto.MatchingDtos.RecipeMatchResponse;
import com.dishcover.matching.service.MatchingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gợi ý công thức theo nguyên liệu — chỉ yêu cầu JWT hợp lệ (Freemium đã gỡ, CLAUDE.md mục 8). */
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
     * <p>Token forward xuống Inventory/Recipe/User lấy từ header Authorization HOẶC cookie
     * {@code auth_token} (trình duyệt dùng httpOnly cookie, không gửi header) — xem
     * {@link RequestTokenExtractor}. Request đã qua JwtAuthFilter nên chắc chắn có token.</p>
     *
     * @param topN số lượng kết quả tối đa mong muốn; có thể null
     * @return danh sách công thức phù hợp, sắp xếp giảm dần theo điểm số
     */
    @GetMapping("/suggestions")
    public List<RecipeMatchResponse> suggestions(HttpServletRequest request,
                                                  @RequestParam(required = false) Integer topN) {
        return service.suggest(RequestTokenExtractor.resolveBearer(request), topN);
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

    /**
     * So số lượng đủ/thiếu từng nguyên liệu của 1 công thức với tủ lạnh người dùng đang đăng nhập —
     * dùng cho màn Chi tiết công thức (vào thẳng từ Tìm kiếm/link, không chỉ từ Gợi ý) và màn xác
     * nhận "Đã nấu xong" (tự điền số lượng sắp trừ kho).
     *
     * @param id id công thức cần kiểm tra
     * @return so sánh đủ/thiếu cho từng nguyên liệu của công thức
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu không tìm thấy công thức
     */
    @GetMapping("/recipes/{id}/availability")
    public RecipeAvailabilityResponse availability(HttpServletRequest request, @PathVariable String id) {
        return service.checkAvailability(id, RequestTokenExtractor.resolveBearer(request));
    }
}
