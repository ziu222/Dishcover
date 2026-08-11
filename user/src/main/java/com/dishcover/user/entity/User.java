package com.dishcover.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Map bảng user_service.users (schema mặc định cấu hình trong application.yml). */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false)
    private String plan = "FREE";

    // DB tự set DEFAULT now(); không ghi từ ứng dụng
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    /** Constructor rỗng bắt buộc cho JPA — không dùng trực tiếp trong code nghiệp vụ. */
    protected User() {
    }

    /**
     * Tạo user mới với plan mặc định FREE.
     *
     * @param email        email đăng nhập, phải là duy nhất trong hệ thống
     * @param passwordHash mật khẩu đã băm (BCrypt), không lưu plaintext
     * @param fullName     họ tên hiển thị, có thể null
     */
    public User(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.plan = "FREE";
    }

    /** @return id định danh của user */
    public Long getId() {
        return id;
    }

    /** @return email đăng nhập */
    public String getEmail() {
        return email;
    }

    /** @return mật khẩu đã băm (BCrypt) */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** @return họ tên hiển thị */
    public String getFullName() {
        return fullName;
    }

    /** Cập nhật họ tên hiển thị. */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /** @return URL ảnh đại diện */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /** Cập nhật URL ảnh đại diện. */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /** @return gói dịch vụ hiện tại (FREE hoặc PRO) */
    public String getPlan() {
        return plan;
    }

    /** Cập nhật gói dịch vụ (FREE hoặc PRO). */
    public void setPlan(String plan) {
        this.plan = plan;
    }

    /** @return thời điểm tạo user, do DB tự set (DEFAULT now()) */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
