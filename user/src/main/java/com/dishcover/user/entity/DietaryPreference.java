package com.dishcover.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Map bảng user_service.dietary_preferences.
 * type = ALLERGY | DIET; value = 'hải sản', 'chay'... (Matching Service đọc để lọc dị ứng).
 */
@Entity
@Table(name = "dietary_preferences")
public class DietaryPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String value;

    /** Constructor rỗng bắt buộc cho JPA — không dùng trực tiếp trong code nghiệp vụ. */
    protected DietaryPreference() {
    }

    /**
     * Tạo một mục hồ sơ ăn uống mới cho user.
     *
     * @param userId id user sở hữu mục này
     * @param type   ALLERGY hoặc DIET
     * @param value  giá trị cụ thể, VD 'hải sản', 'chay'
     */
    public DietaryPreference(Long userId, String type, String value) {
        this.userId = userId;
        this.type = type;
        this.value = value;
    }

    /** @return id định danh của mục hồ sơ ăn uống */
    public Long getId() {
        return id;
    }

    /** @return id user sở hữu mục này */
    public Long getUserId() {
        return userId;
    }

    /** @return loại mục (ALLERGY hoặc DIET) */
    public String getType() {
        return type;
    }

    /** @return giá trị cụ thể của mục ăn uống */
    public String getValue() {
        return value;
    }
}
