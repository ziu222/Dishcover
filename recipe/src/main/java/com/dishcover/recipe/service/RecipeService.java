package com.dishcover.recipe.service;

import com.dishcover.common.ingredient.IngredientCatalog;
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

    private static final double ESSENTIAL_WEIGHT = 1.0;
    private static final double OPTIONAL_WEIGHT = 0.3;

    private final RecipeRepository repo;
    private final IngredientCatalog catalog;

    public RecipeService(RecipeRepository repo, IngredientCatalog catalog) {
        this.repo = repo;
        this.catalog = catalog;
    }

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

    public RecipeDetailResponse getOne(String id) {
        return toDetail(requireById(id));
    }

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
                r.essential() ? ESSENTIAL_WEIGHT : OPTIONAL_WEIGHT
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
