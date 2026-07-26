package com.dishcover.recipe.repository;

import com.dishcover.recipe.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecipeRepository extends MongoRepository<Recipe, String> {

    Page<Recipe> findByTagsContainingIgnoreCase(String tag, Pageable pageable);

    Page<Recipe> findByDifficulty(String difficulty, Pageable pageable);

    Page<Recipe> findByTagsContainingIgnoreCaseAndDifficulty(String tag, String difficulty, Pageable pageable);

    /** So khớp tên đã chuẩn hóa (bỏ dấu) — client search "ca kho" phải ra "Cá kho tộ". */
    Page<Recipe> findByNormalizedNameContaining(String normalizedNameFragment, Pageable pageable);
}
