package com.dishcover.recipe.repository;

import com.dishcover.recipe.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.CountQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * Truy cập collection {@code recipes} trên MongoDB qua Spring Data. Cung cấp các truy vấn lọc
 * theo tag, độ khó và tên chuẩn hóa dùng cho endpoint {@code GET /recipes} (CLAUDE.md mục 3.2).
 */
public interface RecipeRepository extends MongoRepository<Recipe, String> {

    /**
     * Tìm các công thức theo độ khó.
     *
     * @param difficulty  giá trị độ khó (EASY | MEDIUM | HARD)
     * @param pageable    thông tin phân trang/sắp xếp
     * @return trang kết quả công thức khớp độ khó
     */
    Page<Recipe> findByDifficulty(String difficulty, Pageable pageable);

    /**
     * Tìm công thức có {@code value} khớp CHỨA trong {@code tags} HOẶC {@code dietary_flags} —
     * dùng cho lọc theo "định hướng ăn uống" (VD "vegetarian" nằm ở dietary_flags chứ không phải
     * tags, xem specs/diet-direction-recommendation.md mục 7.4: diets[] tách 2 nơi lúc seed). Gộp
     * cả 2 field vì client không cần biết/phân biệt giá trị đang nằm ở field nào khi lọc.
     *
     * @param value     giá trị cần tìm (không phân biệt hoa/thường)
     * @param pageable  thông tin phân trang/sắp xếp
     * @return trang kết quả công thức khớp tags hoặc dietary_flags
     */
    @Query("{ $or: [ { 'tags': { $regex: ?0, $options: 'i' } }, { 'dietary_flags': { $regex: ?0, $options: 'i' } } ] }")
    @CountQuery("{ $or: [ { 'tags': { $regex: ?0, $options: 'i' } }, { 'dietary_flags': { $regex: ?0, $options: 'i' } } ] }")
    Page<Recipe> findByTagsOrDietaryFlagsContainingIgnoreCase(String value, Pageable pageable);

    /** Như {@link #findByTagsOrDietaryFlagsContainingIgnoreCase} nhưng kèm lọc độ khó. */
    @Query("{ $and: [ { $or: [ { 'tags': { $regex: ?0, $options: 'i' } }, { 'dietary_flags': { $regex: ?0, $options: 'i' } } ] }, { 'difficulty': ?1 } ] }")
    @CountQuery("{ $and: [ { $or: [ { 'tags': { $regex: ?0, $options: 'i' } }, { 'dietary_flags': { $regex: ?0, $options: 'i' } } ] }, { 'difficulty': ?1 } ] }")
    Page<Recipe> findByTagsOrDietaryFlagsContainingIgnoreCaseAndDifficulty(String value, String difficulty, Pageable pageable);

    /**
     * So khớp tên đã chuẩn hóa (bỏ dấu) — client search "ca kho" phải ra "Cá kho tộ".
     *
     * @param normalizedNameFragment  đoạn tên đã chuẩn hóa cần tìm
     * @param pageable                thông tin phân trang/sắp xếp
     * @return trang kết quả công thức có tên chuẩn hóa chứa đoạn tìm kiếm
     */
    Page<Recipe> findByNormalizedNameContaining(String normalizedNameFragment, Pageable pageable);
}
