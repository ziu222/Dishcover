package com.dishcover.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Map bảng user_service.calorie_goals — 1-1 với user (unique user_id, upsert khi ghi lại).
 * Chỉ lưu con số cuối cùng, KHÔNG lưu goal_type/preset đã chọn (xem V2__calorie_goals.sql).
 * Matching Service (CalorieProximityRule) đọc để chấm điểm công thức gần mục tiêu.
 */
@Entity
@Table(name = "calorie_goals", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class CalorieGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "calorie_target", nullable = false)
    private Integer calorieTarget;

    @Column(name = "protein_target", nullable = false)
    private Integer proteinTarget;

    @Column(name = "carb_target", nullable = false)
    private Integer carbTarget;

    @Column(name = "fat_target", nullable = false)
    private Integer fatTarget;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Constructor rỗng bắt buộc cho JPA — không dùng trực tiếp trong code nghiệp vụ. */
    protected CalorieGoal() {
    }

    /**
     * Tạo mục tiêu calo/macro/ngày mới cho user.
     *
     * @param userId        id user sở hữu mục tiêu này
     * @param calorieTarget calo mục tiêu/ngày
     * @param proteinTarget đạm mục tiêu/ngày (gram)
     * @param carbTarget    tinh bột mục tiêu/ngày (gram)
     * @param fatTarget     béo mục tiêu/ngày (gram)
     */
    public CalorieGoal(Long userId, Integer calorieTarget, Integer proteinTarget,
                        Integer carbTarget, Integer fatTarget) {
        this.userId = userId;
        this.calorieTarget = calorieTarget;
        this.proteinTarget = proteinTarget;
        this.carbTarget = carbTarget;
        this.fatTarget = fatTarget;
        this.updatedAt = Instant.now();
    }

    /** @return id định danh của mục tiêu */
    public Long getId() {
        return id;
    }

    /** @return id user sở hữu mục tiêu này */
    public Long getUserId() {
        return userId;
    }

    /** @return calo mục tiêu/ngày */
    public Integer getCalorieTarget() {
        return calorieTarget;
    }

    /** @return đạm mục tiêu/ngày (gram) */
    public Integer getProteinTarget() {
        return proteinTarget;
    }

    /** @return tinh bột mục tiêu/ngày (gram) */
    public Integer getCarbTarget() {
        return carbTarget;
    }

    /** @return béo mục tiêu/ngày (gram) */
    public Integer getFatTarget() {
        return fatTarget;
    }

    /**
     * Ghi đè toàn bộ 4 con số mục tiêu (dùng khi upsert — user đã có mục tiêu, gửi lại giá trị mới).
     *
     * @param calorieTarget calo mục tiêu/ngày mới
     * @param proteinTarget đạm mục tiêu/ngày mới (gram)
     * @param carbTarget    tinh bột mục tiêu/ngày mới (gram)
     * @param fatTarget     béo mục tiêu/ngày mới (gram)
     */
    public void update(Integer calorieTarget, Integer proteinTarget, Integer carbTarget, Integer fatTarget) {
        this.calorieTarget = calorieTarget;
        this.proteinTarget = proteinTarget;
        this.carbTarget = carbTarget;
        this.fatTarget = fatTarget;
        this.updatedAt = Instant.now();
    }
}
