package com.dishcover.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Map bảng {@code payment_service.payment_transactions} — mỗi lần bấm "Nâng cấp Pro" tạo một dòng.
 *
 * <p>Khóa chính là UUID và được dùng luôn làm mã đơn gửi sang cổng thanh toán
 * ({@code vnp_TxnRef}). Cố ý không dùng id tự tăng: mã đơn lộ ra ngoài qua URL thanh toán, số tăng
 * dần sẽ để lộ tổng lượng giao dịch của hệ thống và cho phép người ngoài dò đơn của người khác.</p>
 *
 * <p>Ràng buộc {@code UNIQUE (provider, provider_trans_id)} ở tầng DB là chốt chặn cuối chống ghi
 * trùng khi cổng thanh toán gửi IPN nhiều lần (mục 8) — không chỉ dựa vào kiểm tra ở tầng code.</p>
 */
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    /** Trạng thái vòng đời giao dịch, khớp comment trong V1__init.sql. */
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_code", nullable = false)
    private String planCode;

    /**
     * Số tiền chốt tại thời điểm tạo đơn, KHÔNG đọc lại từ bảng plans lúc xác thực IPN — nếu sau
     * này đổi giá gói thì các đơn đang chờ vẫn đối chiếu đúng số tiền người dùng đã thấy.
     */
    @Column(name = "amount_vnd", nullable = false)
    private int amountVnd;

    /** MOMO | VNPAY — để sau này thêm cổng mới không phải đổi lược đồ bảng. */
    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String status = PENDING;

    /** Mã giao dịch phía cổng thanh toán, chỉ có sau khi nhận được kết quả. */
    @Column(name = "provider_trans_id")
    private String providerTransId;

    /**
     * Do ứng dụng gán chứ KHÔNG để {@code insertable = false} dựa vào {@code DEFAULT now()} của
     * Postgres: job quét đơn quá hạn truy vấn theo chính cột này, mà giá trị do DB tự sinh thì
     * không có trong entity vừa lưu (phải refresh mới thấy) và trên H2 lúc test còn là NULL.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(Long userId, String planCode, int amountVnd, String provider) {
        this.userId = userId;
        this.planCode = planCode;
        this.amountVnd = amountVnd;
        this.provider = provider;
        this.status = PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public int getAmountVnd() {
        return amountVnd;
    }

    public String getProvider() {
        return provider;
    }

    public String getStatus() {
        return status;
    }

    public String getProviderTransId() {
        return providerTransId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Chỉ đơn đang PENDING mới được chuyển trạng thái — chặn IPN lặp ghi đè đơn đã chốt. */
    public boolean isPending() {
        return PENDING.equals(status);
    }

    public void markSuccess(String providerTransId) {
        this.status = SUCCESS;
        this.providerTransId = providerTransId;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String providerTransId) {
        this.status = FAILED;
        this.providerTransId = providerTransId;
        this.updatedAt = Instant.now();
    }

    public void markExpired() {
        this.status = EXPIRED;
        this.updatedAt = Instant.now();
    }
}
