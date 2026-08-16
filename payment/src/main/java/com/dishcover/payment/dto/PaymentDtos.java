package com.dishcover.payment.dto;

import com.dishcover.payment.entity.Plan;
import com.dishcover.payment.entity.PaymentTransaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

/** DTO của Payment Service — API luôn trả record, không expose entity JPA (CLAUDE.md mục 9). */
public final class PaymentDtos {

    private PaymentDtos() {
    }

    /**
     * Một gói trong bảng giá.
     *
     * @param code        mã gói (PRO_MONTHLY/PRO_YEARLY)
     * @param priceVnd    giá niêm yết
     * @param durationDays số ngày hiệu lực
     */
    public record PlanResponse(String code, int priceVnd, int durationDays) {
        public static PlanResponse from(Plan p) {
            return new PlanResponse(p.getCode(), p.getPriceVnd(), p.getDurationDays());
        }
    }

    /**
     * Yêu cầu tạo giao dịch.
     *
     * <p>Chỉ nhận {@code planCode} — KHÔNG nhận số tiền từ client. Giá lấy từ bảng giá phía server;
     * để client gửi số tiền là mở đường cho việc mua gói PRO với giá tự đặt (CLAUDE.md mục 8).</p>
     *
     * @param planCode mã gói muốn mua
     */
    public record CheckoutRequest(
            @NotBlank @Pattern(regexp = "[A-Z_]{3,30}", message = "planCode không hợp lệ") String planCode
    ) {
    }

    /**
     * Kết quả tạo giao dịch — client chuyển hướng người dùng sang {@code payUrl}.
     *
     * @param orderId mã đơn, dùng để poll trạng thái sau khi thanh toán
     * @param payUrl  URL cổng thanh toán đã kèm chữ ký
     */
    public record CheckoutResponse(String orderId, String payUrl) {
    }

    /**
     * Trạng thái giao dịch — nguồn sự thật DUY NHẤT để client biết thanh toán thành công hay chưa.
     * Tham số trên URL redirect từ trình duyệt KHÔNG có giá trị xác nhận (mục 11).
     *
     * @param orderId   mã đơn
     * @param planCode  gói đã chọn
     * @param amountVnd số tiền chốt lúc tạo đơn
     * @param status    PENDING | SUCCESS | FAILED | EXPIRED
     * @param createdAt thời điểm tạo đơn
     */
    public record TransactionResponse(
            String orderId, String planCode, int amountVnd, String status, Instant createdAt
    ) {
        public static TransactionResponse from(PaymentTransaction t) {
            return new TransactionResponse(t.getId().toString(), t.getPlanCode(), t.getAmountVnd(),
                    t.getStatus(), t.getCreatedAt());
        }
    }

    /**
     * Tình trạng gói của người dùng hiện tại.
     *
     * @param plan     FREE hoặc PRO
     * @param expireAt thời điểm hết hạn, null nếu đang FREE
     */
    public record MySubscriptionResponse(String plan, Instant expireAt) {
    }
}
