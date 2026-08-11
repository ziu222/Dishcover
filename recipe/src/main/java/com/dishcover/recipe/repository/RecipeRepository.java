package com.dishcover.recipe.repository;

import com.dishcover.recipe.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Truy cập collection {@code recipes} trên MongoDB qua Spring Data. Cung cấp các truy vấn lọc
 * theo tag, độ khó và tên chuẩn hóa dùng cho endpoint {@code GET /recipes} (CLAUDE.md mục 3.2).
 */
public interface RecipeRepository extends MongoRepository<Recipe, String> {

    /**
     * Tìm các công thức có chứa tag chỉ định (không phân biệt hoa/thường).
     *
     * @param tag       tag cần lọc
     * @param pageable  thông tin phân trang/sắp xếp
     * @return trang kết quả công thức khớp tag
     */
    Page<Recipe> findByTagsContainingIgnoreCase(String tag, Pageable pageable);

    /**
     * Tìm các công thức theo độ khó.
     *
     * @param difficulty  giá trị độ khó (EASY | MEDIUM | HARD)
     * @param pageable    thông tin phân trang/sắp xếp
     * @return trang kết quả công thức khớp độ khó
     */
    Page<Recipe> findByDifficulty(String difficulty, Pageable pageable);

    /**
     * Tìm các công thức khớp đồng thời cả tag (không phân biệt hoa/thường) và độ khó.
     *
     * @param tag         tag cần lọc
     * @param difficulty  giá trị độ khó (EASY | MEDIUM | HARD)
     * @param pageable    thông tin phân trang/sắp xếp
     * @return trang kết quả công thức khớp cả hai điều kiện
     */
    Page<Recipe> findByTagsContainingIgnoreCaseAndDifficulty(String tag, String difficulty, Pageable pageable);

    /**
     * So khớp tên đã chuẩn hóa (bỏ dấu) — client search "ca kho" phải ra "Cá kho tộ".
     *
     * @param normalizedNameFragment  đoạn tên đã chuẩn hóa cần tìm
     * @param pageable                thông tin phân trang/sắp xếp
     * @return trang kết quả công thức có tên chuẩn hóa chứa đoạn tìm kiếm
     */
    Page<Recipe> findByNormalizedNameContaining(String normalizedNameFragment, Pageable pageable);
}
