package com.dishcover.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Map bảng {@code payment_service.subscriptions} — quyền dùng gói PRO theo khoảng thời gian.
 *
 * <p>Mỗi lần thanh toán thành công tạo một dòng mới thay vì sửa dòng cũ ({@code source_transaction_id}
 * trỏ ngược về giao dịch sinh ra nó). Giữ lịch sử đầy đủ như vậy thì gia hạn, hoàn tiền hay tra soát
 * khiếu nại đều lần lại được, và cũng là cách để cột {@code source_transaction_id NOT NULL} trong
 * lược đồ có ý nghĩa: không có subscription nào tồn tại mà không truy ra được đơn đã trả tiền.</p>
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    public static final String ACTIVE = "ACTIVE";
    public static final String EXPIRED = "EXPIRED";
    public static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_code", nullable = false)
    private String planCode;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(nullable = false)
    private String status = ACTIVE;

    @Column(name = "source_transaction_id", nullable = false)
    private UUID sourceTransactionId;

    protected Subscription() {
    }

    public Subscription(Long userId, String planCode, Instant startAt, Instant endAt, UUID sourceTransactionId) {
        this.userId = userId;
        this.planCode = planCode;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = ACTIVE;
        this.sourceTransactionId = sourceTransactionId;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public String getStatus() {
        return status;
    }

    public UUID getSourceTransactionId() {
        return sourceTransactionId;
    }

    /**
     * Còn hiệu lực hay không tính theo mốc thời gian truyền vào, KHÔNG chỉ đọc cột {@code status}:
     * gói hết hạn theo thời gian chứ không nhờ ai đó chạy job đổi cờ, nên cứ đọc {@code status}
     * suông sẽ cho PRO lố hạn nếu job chưa kịp chạy.
     */
    public boolean isActiveAt(Instant moment) {
        return ACTIVE.equals(status) && !moment.isBefore(startAt) && moment.isBefore(endAt);
    }

    public void cancel() {
        this.status = CANCELLED;
    }
}
