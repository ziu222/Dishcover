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
     * khi expiryDate null thì luôn tạo dòng mới, không tìm lô cũ). Trả về {@code List} chứ không
     * phải {@code Optional} vì KHÔNG có UNIQUE constraint ở tầng DB — có thể có nhiều dòng cùng
     * khoá (VD khác đơn vị tính, hoặc trước đây từng race tạo trùng); InventoryService tự chọn
     * dòng phù hợp để gộp. Xem InventoryService.upsert().
     */
    List<UserIngredient> findByUserIdAndNormalizedNameAndExpiryDate(
            Long userId, String normalizedName, LocalDate expiryDate);

    /**
     * Mọi lô của 1 nguyên liệu thuộc 1 user, KHÔNG lọc theo status ở tầng query — status hiển thị
     * là derived (xem javadoc lớp {@code InventoryService}), lọc/sắp xếp FEFO thực hiện ở tầng
     * service sau khi derive, không tin cột status thô.
     *
     * @param userId id người dùng
     * @param normalizedName tên nguyên liệu đã chuẩn hóa
     * @return mọi lô (kể cả USED/EXPIRED thô) của nguyên liệu này, chưa sắp xếp
     */
    List<UserIngredient> findByUserIdAndNormalizedName(Long userId, String normalizedName);

    /**
     * Ứng viên cho quét cảnh báo hết hạn hàng ngày ({@code IngredientExpiryScanner}): mọi dòng
     * chưa bị đánh dấu USED và có hạn dùng trong ngưỡng — NULL ở {@code expiryDate} tự động bị
     * loại vì SQL {@code NULL <= threshold} luôn false, không cần điều kiện IS NOT NULL riêng.
     *
     * @param status    trạng thái LƯU TRỮ (thô) cần loại trừ — luôn truyền {@code "USED"}
     * @param threshold ngày xa nhất còn tính là "sắp hết hạn" (hôm nay + NEAR_EXPIRY_DAYS)
     */
    List<UserIngredient> findByStatusNotAndExpiryDateLessThanEqual(String status, LocalDate threshold);
}
