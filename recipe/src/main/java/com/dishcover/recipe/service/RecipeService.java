package com.dishcover.recipe.service;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientWeights;
import com.dishcover.common.text.VietnameseTextNormalizer;
import com.dishcover.recipe.dto.RecipeDtos.CreateRecipeRequest;
import com.dishcover.recipe.dto.RecipeDtos.RecipeDetailResponse;
import com.dishcover.recipe.dto.RecipeDtos.RecipeIngredientRequest;
import com.dishcover.recipe.dto.RecipeDtos.RecipeIngredientResponse;
import com.dishcover.recipe.dto.RecipeDtos.RecipeStepRequest;
import com.dishcover.recipe.dto.RecipeDtos.RecipeStepResponse;
import com.dishcover.recipe.dto.RecipeDtos.RecipeSummaryResponse;
import com.dishcover.recipe.dto.RecipeDtos.UpdateRecipeRequest;
import com.dishcover.recipe.entity.Recipe;
import com.dishcover.recipe.entity.RecipeIngredient;
import com.dishcover.recipe.entity.RecipeStep;
import com.dishcover.common.exception.ResourceNotFoundException;
import com.dishcover.recipe.repository.RecipeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Business rule đầy đủ ở specs/recipe-service.md mục 3.3: chuẩn hóa ingredient lúc ghi (KHÔNG tin
 * client gửi normalizedName), essential->weight tự tính (1.0/0.3, không nhận weight tùy ý từ client),
 * slug tự sinh nếu thiếu. KHÔNG gọi Matching Service — chưa tồn tại (xem spec mục 3.3).
 */
@Service
public class RecipeService {

    private final RecipeRepository repo;
    private final IngredientCatalog catalog;

    /**
     * @param repo    repository truy cập collection {@code recipes} trên MongoDB
     * @param catalog catalog nguyên liệu chuẩn dùng để chuẩn hóa tên nguyên liệu client gửi lên
     */
    public RecipeService(RecipeRepository repo, IngredientCatalog catalog) {
        this.repo = repo;
        this.catalog = catalog;
    }

    /**
     * Liệt kê công thức có phân trang, ưu tiên lọc theo từ khóa tên nếu có, kế đến tổ hợp
     * tag+độ khó, rồi từng điều kiện riêng lẻ, cuối cùng là liệt kê tất cả.
     *
     * @param tag         tag cần lọc, có thể null/rỗng
     * @param difficulty  độ khó cần lọc, có thể null/rỗng
     * @param query       từ khóa tên (sẽ được chuẩn hóa bỏ dấu trước khi so khớp), có thể null/rỗng
     * @param pageable    thông tin phân trang/sắp xếp
     * @return trang danh sách công thức dạng tóm tắt
     */
    public Page<RecipeSummaryResponse> list(String tag, String difficulty, String query, Pageable pageable) {
        Page<Recipe> page;
        if (StringUtils.hasText(query)) {
            page = repo.findByNormalizedNameContaining(VietnameseTextNormalizer.normalize(query), pageable);
        } else if (StringUtils.hasText(tag) && StringUtils.hasText(difficulty)) {
            page = repo.findByTagsContainingIgnoreCaseAndDifficulty(tag, difficulty, pageable);
        } else if (StringUtils.hasText(tag)) {
            page = repo.findByTagsContainingIgnoreCase(tag, pageable);
        } else if (StringUtils.hasText(difficulty)) {
            page = repo.findByDifficulty(difficulty, pageable);
        } else {
            page = repo.findAll(pageable);
        }
        return page.map(this::toSummary);
    }

    /**
     * @param id id công thức
     * @return chi tiết công thức
     * @throws ResourceNotFoundException nếu không tìm thấy công thức với id chỉ định
     */
    public RecipeDetailResponse getOne(String id) {
        return toDetail(requireById(id));
    }

    /**
     * Tạo mới một công thức. Sinh id ngẫu nhiên (UUID), tự sinh slug từ tên nếu client không gửi,
     * chuẩn hóa từng nguyên liệu qua {@link IngredientCatalog#resolve} (không tin normalizedName
     * client gửi) và tự tính weight từ cờ essential (KHÔNG nhận weight tùy ý từ client).
     *
     * @param req payload tạo công thức
     * @return chi tiết công thức vừa tạo
     */
    public RecipeDetailResponse create(CreateRecipeRequest req) {
        String id = UUID.randomUUID().toString();
        String slug = StringUtils.hasText(req.slug()) ? req.slug() : generateSlug(req.name());
        Recipe recipe = new Recipe(
                id, req.name(), slug, req.cookTimeMinutes(), req.difficulty(),
                req.tags(), req.dietaryFlags(),
                toEntityIngredients(req.ingredients()), toEntitySteps(req.steps()),
                req.imageUrl(), req.videoUrl());
        recipe.setNormalizedName(VietnameseTextNormalizer.normalize(req.name()));
        return toDetail(repo.save(recipe));
    }

    /**
     * Cập nhật một phần công thức — chỉ áp field nào client thực sự gửi (khác null). Nguyên liệu
     * gửi lên (nếu có) được chuẩn hóa lại toàn bộ giống như lúc tạo mới.
     *
     * @param id  id công thức cần cập nhật
     * @param req payload chứa các field cần thay đổi
     * @return chi tiết công thức sau khi cập nhật
     * @throws ResourceNotFoundException nếu không tìm thấy công thức với id chỉ định
     */
    public RecipeDetailResponse update(String id, UpdateRecipeRequest req) {
        Recipe recipe = requireById(id);
        if (req.name() != null) {
            recipe.setName(req.name());
            recipe.setNormalizedName(VietnameseTextNormalizer.normalize(req.name()));
        }
        if (req.cookTimeMinutes() != null) {
            recipe.setCookTimeMinutes(req.cookTimeMinutes());
        }
        if (req.difficulty() != null) {
            recipe.setDifficulty(req.difficulty());
        }
        if (req.tags() != null) {
            recipe.setTags(req.tags());
        }
        if (req.dietaryFlags() != null) {
            recipe.setDietaryFlags(req.dietaryFlags());
        }
        if (req.ingredients() != null) {
            recipe.setIngredients(toEntityIngredients(req.ingredients()));
        }
        if (req.steps() != null) {
            recipe.setSteps(toEntitySteps(req.steps()));
        }
        if (req.imageUrl() != null) {
            recipe.setImageUrl(req.imageUrl());
        }
        if (req.videoUrl() != null) {
            recipe.setVideoUrl(req.videoUrl());
        }
        return toDetail(repo.save(recipe));
    }

    /**
     * Xóa một công thức theo id.
     *
     * @param id id công thức cần xóa
     * @throws ResourceNotFoundException nếu không tìm thấy công thức với id chỉ định
     */
    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy công thức id=" + id);
        }
        repo.deleteById(id);
    }

    // ---- helpers ----

    private Recipe requireById(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức id=" + id));
    }

    private List<RecipeIngredient> toEntityIngredients(List<RecipeIngredientRequest> reqs) {
        return reqs.stream().map(r -> new RecipeIngredient(
                generateIngredientId(r.name()),
                r.name(),
                catalog.resolve(r.name()),
                r.amount(),
                r.unit(),
                r.essential(),
                r.essential() ? IngredientWeights.ESSENTIAL : IngredientWeights.OPTIONAL
        )).toList();
    }

    private List<RecipeStep> toEntitySteps(List<RecipeStepRequest> reqs) {
        return reqs.stream()
                .map(r -> new RecipeStep(r.order(), r.title(), r.content(),
                        r.durationMinutes() != null ? r.durationMinutes() : 0))
                .toList();
    }

    private String generateSlug(String name) {
        return VietnameseTextNormalizer.normalize(name).replace(' ', '-');
    }

    private String generateIngredientId(String name) {
        return "ing_" + VietnameseTextNormalizer.normalize(name).replace(' ', '_');
    }

    private RecipeSummaryResponse toSummary(Recipe r) {
        return new RecipeSummaryResponse(r.getId(), r.getName(), r.getSlug(), r.getCookTimeMinutes(),
                r.getDifficulty(), r.getTags(), r.getImageUrl());
    }

    private RecipeDetailResponse toDetail(Recipe r) {
        List<RecipeIngredientResponse> ingredients = r.getIngredients().stream()
                .map(i -> new RecipeIngredientResponse(
                        i.getName(), i.getNormalizedName(), i.getAmount(), i.getUnit(), i.isEssential(), i.getWeight()))
                .toList();
        List<RecipeStepResponse> steps = r.getSteps().stream()
                .map(s -> new RecipeStepResponse(s.getOrder(), s.getTitle(), s.getContent(), s.getDurationMinutes()))
                .toList();
        return new RecipeDetailResponse(r.getId(), r.getName(), r.getSlug(), r.getCookTimeMinutes(),
                r.getDifficulty(), r.getTags(), r.getDietaryFlags(), ingredients, steps,
                r.getImageUrl(), r.getVideoUrl());
    }
}
