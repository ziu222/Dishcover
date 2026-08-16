package com.dishcover.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Map bảng {@code payment_service.plans} — bảng giá các gói PRO.
 *
 * <p>Khóa chính là mã gói dạng chuỗi ({@code PRO_MONTHLY}/{@code PRO_YEARLY}) chứ không phải id
 * tự tăng: mã gói xuất hiện trong request checkout của client và trong bản ghi giao dịch, để dạng
 * chuỗi đọc hiểu được thì log và dữ liệu thanh toán truy vết dễ hơn nhiều so với số.</p>
 */
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    private String code;

    /** Giá niêm yết bằng VND. Đây là nguồn sự thật để đối chiếu số tiền trong IPN (mục 8). */
    @Column(name = "price_vnd", nullable = false)
    private int priceVnd;

    /** Số ngày hiệu lực, dùng tính {@code end_at} của subscription khi kích hoạt. */
    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    /** Gói ngừng bán thì tắt cờ này thay vì xóa dòng — giao dịch cũ vẫn tham chiếu tới nó. */
    @Column(nullable = false)
    private boolean active = true;

    protected Plan() {
    }

    public Plan(String code, int priceVnd, int durationDays) {
        this.code = code;
        this.priceVnd = priceVnd;
        this.durationDays = durationDays;
    }

    public String getCode() {
        return code;
    }

    public int getPriceVnd() {
        return priceVnd;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
