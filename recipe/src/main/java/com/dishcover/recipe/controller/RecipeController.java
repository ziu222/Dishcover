package com.dishcover.recipe.controller;

import com.dishcover.common.security.RequestTokenExtractor;
import com.dishcover.recipe.dto.RecipeDtos.CreateRecipeRequest;
import com.dishcover.recipe.dto.RecipeDtos.RecipeDetailResponse;
import com.dishcover.recipe.dto.RecipeDtos.RecipeSummaryResponse;
import com.dishcover.recipe.dto.RecipeDtos.UpdateRecipeRequest;
import com.dishcover.recipe.service.RecipeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** GET công khai (FREE), POST/PATCH/DELETE cần JWT (specs/recipe-service.md mục 3.2). */
@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService service;

    /**
     * @param service tầng nghiệp vụ xử lý CRUD công thức
     */
    public RecipeController(RecipeService service) {
        this.service = service;
    }

    /**
     * Liệt kê công thức có phân trang, lọc theo tag/độ khó/từ khóa tên. Endpoint công khai,
     * không cần JWT (CLAUDE.md mục 8).
     *
     * @param tag         tag cần lọc, có thể null
     * @param difficulty  độ khó cần lọc (EASY | MEDIUM | HARD), có thể null
     * @param q           từ khóa tìm theo tên (so khớp không phân biệt dấu), có thể null
     * @param pageable    thông tin phân trang/sắp xếp lấy từ query param
     * @return trang danh sách công thức dạng tóm tắt
     */
    @GetMapping
    public Page<RecipeSummaryResponse> list(@RequestParam(required = false) String tag,
                                             @RequestParam(required = false) String difficulty,
                                             @RequestParam(required = false) String q,
                                             Pageable pageable) {
        return service.list(tag, difficulty, q, pageable);
    }

    /**
     * Lấy chi tiết một công thức theo id. Endpoint công khai, không cần JWT.
     *
     * @param id id công thức
     * @return chi tiết công thức bao gồm nguyên liệu và các bước nấu
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu không tìm thấy công thức
     */
    @GetMapping("/{id}")
    public RecipeDetailResponse getOne(@PathVariable String id) {
        return service.getOne(id);
    }

    /**
     * Tạo mới một công thức. Yêu cầu JWT hợp lệ.
     *
     * @param req payload tạo công thức, đã qua validate {@code @Valid}
     * @return response 201 kèm chi tiết công thức vừa tạo
     */
    @PostMapping
    public ResponseEntity<RecipeDetailResponse> create(HttpServletRequest httpRequest,
                                                        @Valid @RequestBody CreateRecipeRequest req) {
        String bearerToken = RequestTokenExtractor.resolveBearer(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, bearerToken));
    }

    /**
     * Cập nhật một phần công thức — chỉ áp field nào client thực sự gửi. Yêu cầu JWT hợp lệ.
     *
     * @param id  id công thức cần cập nhật
     * @param req payload chứa các field cần thay đổi, đã qua validate {@code @Valid}
     * @return chi tiết công thức sau khi cập nhật
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu không tìm thấy công thức
     */
    @PatchMapping("/{id}")
    public RecipeDetailResponse update(HttpServletRequest httpRequest, @PathVariable String id,
                                        @Valid @RequestBody UpdateRecipeRequest req) {
        String bearerToken = RequestTokenExtractor.resolveBearer(httpRequest);
        return service.update(id, req, bearerToken);
    }

    /**
     * Xóa một công thức theo id. Yêu cầu JWT hợp lệ.
     *
     * @param id id công thức cần xóa
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu không tìm thấy công thức
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
