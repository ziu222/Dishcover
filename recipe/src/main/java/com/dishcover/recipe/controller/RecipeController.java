package com.dishcover.recipe.controller;

import com.dishcover.recipe.dto.RecipeDtos.CreateRecipeRequest;
import com.dishcover.recipe.dto.RecipeDtos.RecipeDetailResponse;
import com.dishcover.recipe.dto.RecipeDtos.RecipeSummaryResponse;
import com.dishcover.recipe.dto.RecipeDtos.UpdateRecipeRequest;
import com.dishcover.recipe.service.RecipeService;
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

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    @GetMapping
    public Page<RecipeSummaryResponse> list(@RequestParam(required = false) String tag,
                                             @RequestParam(required = false) String difficulty,
                                             @RequestParam(required = false) String q,
                                             Pageable pageable) {
        return service.list(tag, difficulty, q, pageable);
    }

    @GetMapping("/{id}")
    public RecipeDetailResponse getOne(@PathVariable String id) {
        return service.getOne(id);
    }

    @PostMapping
    public ResponseEntity<RecipeDetailResponse> create(@Valid @RequestBody CreateRecipeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PatchMapping("/{id}")
    public RecipeDetailResponse update(@PathVariable String id, @Valid @RequestBody UpdateRecipeRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
