package com.dishcover.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Map bảng inventory_service.user_ingredients (schema mặc định cấu hình trong application.yml). */
@Entity
@Table(name = "user_ingredients")
public class UserIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ingredient_name", nullable = false)
    private String ingredientName;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    private BigDecimal quantity;

    private String unit;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(nullable = false)
    private String source = "MANUAL";

    @Column(nullable = false)
    private String status = "FRESH";

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Constructor rỗng bắt buộc bởi JPA — không dùng trực tiếp trong code nghiệp vụ. */
    protected UserIngredient() {
    }

    /**
     * Tạo một dòng nguyên liệu mới (một "lô hàng") thuộc một người dùng.
     *
     * @param userId id người dùng sở hữu dòng nguyên liệu này
     * @param ingredientName tên nguyên liệu thô do người dùng nhập hoặc nhận diện ảnh trả về
     * @param normalizedName tên đã chuẩn hóa qua Ingredient Catalog, dùng làm khóa so khớp
     *                        khi gộp lô (xem {@code InventoryService})
     * @param quantity số lượng
     * @param unit đơn vị tính
     * @param expiryDate hạn sử dụng, có thể null nếu không xác định
     * @param source nguồn nhập liệu: {@code MANUAL} hoặc {@code IMAGE_RECOGNITION}
     * @param status trạng thái ban đầu (thường là {@code FRESH})
     */
    public UserIngredient(Long userId, String ingredientName, String normalizedName,
                          BigDecimal quantity, String unit, LocalDate expiryDate,
                          String source, String status) {
        this.userId = userId;
        this.ingredientName = ingredientName;
        this.normalizedName = normalizedName;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
        this.source = source;
        this.status = status;
    }

    /** @return id của dòng nguyên liệu (khóa chính) */
    public Long getId() {
        return id;
    }

    /** @return id người dùng sở hữu dòng nguyên liệu */
    public Long getUserId() {
        return userId;
    }

    /** @return tên nguyên liệu thô (chưa chuẩn hóa) */
    public String getIngredientName() {
        return ingredientName;
    }

    /** @return tên đã chuẩn hóa, dùng làm khóa so khớp giữa Inventory và Recipe/Matching */
    public String getNormalizedName() {
        return normalizedName;
    }

    /** @return số lượng hiện có */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /**
     * Cập nhật số lượng và đánh dấu {@code updatedAt} là thời điểm hiện tại.
     *
     * @param quantity số lượng mới
     */
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        this.updatedAt = Instant.now();
    }

    /** @return đơn vị tính */
    public String getUnit() {
        return unit;
    }

    /**
     * Cập nhật đơn vị tính và đánh dấu {@code updatedAt} là thời điểm hiện tại.
     *
     * @param unit đơn vị tính mới
     */
    public void setUnit(String unit) {
        this.unit = unit;
        this.updatedAt = Instant.now();
    }

    /** @return hạn sử dụng, có thể null */
    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    /**
     * Cập nhật hạn sử dụng và đánh dấu {@code updatedAt} là thời điểm hiện tại. Không tự
     * tính lại {@code status} — việc derive status theo hạn dùng mới thuộc về tầng service.
     *
     * @param expiryDate hạn sử dụng mới
     */
    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
        this.updatedAt = Instant.now();
    }

    /** @return nguồn nhập liệu ({@code MANUAL} hoặc {@code IMAGE_RECOGNITION}) */
    public String getSource() {
        return source;
    }

    /**
     * @return trạng thái đang được lưu trong DB. Đây là giá trị lưu trữ thô, KHÔNG phải
     *         status hiển thị cho client — status hiển thị (FRESH/EXPIRING_SOON/EXPIRED/USED)
     *         được tính lại (derived) dựa trên hạn dùng ở tầng service mỗi lần map response,
     *         không phải bằng cron job cập nhật cột này.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Ghi đè trạng thái lưu trữ và đánh dấu {@code updatedAt} là thời điểm hiện tại.
     * Dùng khi người dùng chủ động đổi status (VD đánh dấu {@code USED}).
     *
     * @param status trạng thái mới
     */
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    /** @return thời điểm tạo dòng, do DB tự sinh (cột {@code insertable = false}) */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** @return thời điểm cập nhật gần nhất, null nếu chưa từng sửa sau khi tạo */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
