package com.dishcover.inventory.repository;

import com.dishcover.inventory.entity.UserIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA cho {@link UserIngredient}, thao tác trên bảng
 * {@code inventory_service.user_ingredients}.
 */
public interface UserIngredientRepository extends JpaRepository<UserIngredient, Long> {

    /**
     * Lấy toàn bộ nguyên liệu của một người dùng, không lọc theo trạng thái.
     *
     * @param userId id người dùng
     * @return danh sách nguyên liệu, rỗng nếu người dùng chưa có gì
     */
    List<UserIngredient> findByUserId(Long userId);

    /**
     * Lấy nguyên liệu của một người dùng đang có trạng thái lưu trữ khớp {@code status}.
     * Lưu ý: đây là trạng thái lưu trong DB, không phải status derived (xem
     * {@code InventoryService}).
     *
     * @param userId id người dùng
     * @param status giá trị trạng thái lưu trữ cần lọc
     * @return danh sách nguyên liệu khớp điều kiện
     */
    List<UserIngredient> findByUserIdAndStatus(Long userId, String status);

    /** Ownership check: chỉ trả về nếu dòng thuộc đúng user — dùng cho GET/PATCH/DELETE 1 item. */
    Optional<UserIngredient> findByIdAndUserId(Long id, Long userId);

    /**
     * Xóa một dòng nguyên liệu, chỉ khi thuộc đúng người dùng sở hữu (ownership check).
     *
     * @param id id dòng nguyên liệu
     * @param userId id người dùng yêu cầu xóa
     * @return số dòng bị xóa (0 nếu id không tồn tại hoặc không thuộc người dùng)
     */
    long deleteByIdAndUserId(Long id, Long userId);

    /**
     * Cùng lô hàng = cùng user + cùng nguyên liệu + cùng hạn dùng (kể cả null — dùng
     * "IS NOT DISTINCT FROM" qua @Query nếu cần, nhưng đơn giản hoá: chỉ coalesce ở tầng service
     * khi expiryDate null thì luôn tạo dòng mới, không tìm lô cũ). Xem InventoryService.
     */
    Optional<UserIngredient> findByUserIdAndNormalizedNameAndExpiryDate(
            Long userId, String normalizedName, LocalDate expiryDate);
}
