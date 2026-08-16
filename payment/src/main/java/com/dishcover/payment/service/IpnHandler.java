package com.dishcover.payment.service;

import com.dishcover.payment.entity.PaymentTransaction;
import com.dishcover.payment.entity.Plan;
import com.dishcover.payment.provider.PaymentProvider;
import com.dishcover.payment.repository.PaymentTransactionRepository;
import com.dishcover.payment.repository.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Xử lý IPN — đường DUY NHẤT kích hoạt gói PRO (CLAUDE.md mục 8 bước 5, mục 11).
 *
 * <p>Thứ tự kiểm tra là cố ý và không được đảo: <b>xác thực chữ ký trước mọi thứ khác</b>. Đọc
 * đơn ra khỏi DB rồi mới verify nghĩa là để dữ liệu chưa tin cậy điều khiển truy vấn của mình.</p>
 *
 * <p>Mã phản hồi trả về theo quy ước VNPay: {@code 00} thành công, {@code 01} không thấy đơn,
 * {@code 02} đơn đã xác nhận rồi, {@code 04} sai số tiền, {@code 97} sai chữ ký, {@code 99} lỗi khác.
 * Luôn trả HTTP 200 kèm mã này — trả lỗi HTTP sẽ khiến VNPay gửi lại liên tục.</p>
 */
@Service
public class IpnHandler {

    private static final Logger log = LoggerFactory.getLogger(IpnHandler.class);

    private final PaymentTransactionRepository transactions;
    private final PlanRepository plans;
    private final PaymentProvider provider;
    private final SubscriptionManager subscriptionManager;

    public IpnHandler(PaymentTransactionRepository transactions, PlanRepository plans,
                      PaymentProvider provider, SubscriptionManager subscriptionManager) {
        this.transactions = transactions;
        this.plans = plans;
        this.provider = provider;
        this.subscriptionManager = subscriptionManager;
    }

    /** Kết quả trả cho cổng thanh toán. */
    public record IpnResult(String rspCode, String message) {
        static IpnResult of(String code, String message) {
            return new IpnResult(code, message);
        }
    }

    /**
     * Xử lý một thông báo IPN.
     *
     * <p>Chạy trong một transaction để "đánh dấu đơn thành công" và "cấp quyền PRO" hoặc cùng
     * thành công hoặc cùng thất bại — nửa vời sẽ tạo ra đơn đã trả tiền mà không có quyền, hoặc
     * quyền không truy ra được đơn nào.</p>
     */
    @Transactional
    public IpnResult handle(Map<String, String> params) {
        // 1. CHỮ KÝ TRƯỚC TIÊN — chưa verify thì mọi tham số còn lại đều là dữ liệu của người lạ.
        if (!provider.verifyCallback(params)) {
            log.warn("IPN bị từ chối: chữ ký không hợp lệ");
            return IpnResult.of("97", "Invalid signature");
        }

        Optional<PaymentTransaction> found = parseOrderId(params).flatMap(transactions::findById);
        if (found.isEmpty()) {
            log.warn("IPN: không tìm thấy đơn {}", provider.extractOrderId(params));
            return IpnResult.of("01", "Order not found");
        }
        PaymentTransaction tx = found.get();

        // 2. Đối chiếu số tiền với đơn đã lưu — chống sửa giá phía client (mục 8).
        long paid = provider.extractAmountVnd(params);
        if (paid != tx.getAmountVnd()) {
            log.warn("IPN: số tiền lệch cho đơn {} (nhận {}, đơn {})", tx.getId(), paid, tx.getAmountVnd());
            return IpnResult.of("04", "Invalid amount");
        }

        // 3. Idempotent: đơn đã chốt thì không xử lý lại, không cấp thêm quyền lần nữa.
        if (!tx.isPending()) {
            log.info("IPN lặp cho đơn {} đã ở trạng thái {}", tx.getId(), tx.getStatus());
            return IpnResult.of("02", "Order already confirmed");
        }

        String providerTransId = provider.extractProviderTransId(params);
        if (!provider.isSuccessful(params)) {
            tx.markFailed(providerTransId);
            transactions.save(tx);
            log.info("IPN: đơn {} thất bại phía cổng thanh toán", tx.getId());
            // Vẫn trả 00: mình đã ghi nhận thông báo thành công, không cần VNPay gửi lại.
            return IpnResult.of("00", "Confirm Success");
        }

        // 4. Tới đây mới được cấp quyền: chữ ký hợp lệ, đúng đơn, đúng tiền, chưa xử lý lần nào.
        Plan plan = plans.findById(tx.getPlanCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Đơn " + tx.getId() + " trỏ tới gói không tồn tại: " + tx.getPlanCode()));

        tx.markSuccess(providerTransId);
        transactions.save(tx);
        subscriptionManager.activate(tx.getUserId(), plan, tx.getId());
        log.info("IPN: kích hoạt gói {} cho user {} từ đơn {}", plan.getCode(), tx.getUserId(), tx.getId());

        return IpnResult.of("00", "Confirm Success");
    }

    private Optional<UUID> parseOrderId(Map<String, String> params) {
        try {
            return Optional.ofNullable(provider.extractOrderId(params)).map(UUID::fromString);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
