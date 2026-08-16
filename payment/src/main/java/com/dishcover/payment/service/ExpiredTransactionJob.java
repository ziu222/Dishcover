package com.dishcover.payment.service;

import com.dishcover.payment.config.VnpayProperties;
import com.dishcover.payment.entity.PaymentTransaction;
import com.dishcover.payment.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Chuyển đơn treo quá lâu sang EXPIRED (CLAUDE.md mục 8).
 *
 * <p>Người dùng bấm thanh toán rồi bỏ ngang là chuyện thường; không dọn thì bảng giao dịch đầy
 * đơn PENDING vĩnh viễn và màn hình tra cứu của họ treo mãi ở trạng thái "đang chờ".</p>
 *
 * <p>Ngưỡng lấy đúng {@code vnpay.expire-minutes} — cùng mốc đã gửi cho VNPay qua
 * {@code vnp_ExpireDate}. Đặt hai con số rời nhau thì sẽ tới lúc mình coi đơn là hết hạn trong
 * khi VNPay vẫn cho thanh toán, và IPN hợp lệ về tới nơi lại bị chính mình từ chối.</p>
 */
@Service
public class ExpiredTransactionJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiredTransactionJob.class);

    /** Chạy mỗi 5 phút — đơn quá hạn không phải việc gấp, quét dày chỉ tốn truy vấn. */
    private static final long INTERVAL_MS = 5 * 60 * 1000L;

    private final PaymentTransactionRepository transactions;
    private final VnpayProperties props;
    private final Clock clock;

    public ExpiredTransactionJob(PaymentTransactionRepository transactions, VnpayProperties props, Clock clock) {
        this.transactions = transactions;
        this.props = props;
        this.clock = clock;
    }

    /**
     * Quét và đánh dấu hết hạn.
     *
     * <p>Chỉ đụng tới đơn còn PENDING, nên IPN về muộn cho một đơn đã SUCCESS không bị job này ghi
     * đè. Trường hợp ngược lại — job đánh dấu EXPIRED xong IPN mới tới — thì {@code IpnHandler}
     * sẽ trả mã "đơn đã xác nhận" và không cấp quyền; đó là lý do ngưỡng ở đây phải bằng đúng
     * mốc hết hạn đã gửi VNPay.</p>
     *
     * @return số đơn vừa chuyển sang EXPIRED (dùng cho test và log)
     */
    @Scheduled(fixedRate = INTERVAL_MS)
    @Transactional
    public int expireStalePending() {
        Instant threshold = clock.instant().minus(props.expireMinutes(), ChronoUnit.MINUTES);
        List<PaymentTransaction> stale =
                transactions.findByStatusAndCreatedAtBefore(PaymentTransaction.PENDING, threshold);

        stale.forEach(PaymentTransaction::markExpired);
        if (!stale.isEmpty()) {
            transactions.saveAll(stale);
            log.info("Đánh dấu hết hạn {} đơn thanh toán treo quá {} phút", stale.size(), props.expireMinutes());
        }
        return stale.size();
    }
}
