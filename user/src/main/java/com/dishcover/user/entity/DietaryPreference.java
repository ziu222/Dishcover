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

    protected DietaryPreference() {
    }

    public DietaryPreference(Long userId, String type, String value) {
        this.userId = userId;
        this.type = type;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
