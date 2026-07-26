package com.dishcover.inventory.repository;

import com.dishcover.inventory.entity.UserIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserIngredientRepository extends JpaRepository<UserIngredient, Long> {

    List<UserIngredient> findByUserId(Long userId);

    List<UserIngredient> findByUserIdAndStatus(Long userId, String status);

    /** Ownership check: chỉ trả về nếu dòng thuộc đúng user — dùng cho GET/PATCH/DELETE 1 item. */
    Optional<UserIngredient> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    /**
     * Cùng lô hàng = cùng user + cùng nguyên liệu + cùng hạn dùng (kể cả null — dùng
     * "IS NOT DISTINCT FROM" qua @Query nếu cần, nhưng đơn giản hoá: chỉ coalesce ở tầng service
     * khi expiryDate null thì luôn tạo dòng mới, không tìm lô cũ). Xem InventoryService.
     */
    Optional<UserIngredient> findByUserIdAndNormalizedNameAndExpiryDate(
            Long userId, String normalizedName, LocalDate expiryDate);
}
