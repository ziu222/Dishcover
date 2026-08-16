package com.dishcover.payment.service;

import com.dishcover.payment.entity.Plan;
import com.dishcover.payment.entity.Subscription;
import com.dishcover.payment.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Cấp và tra cứu quyền dùng gói PRO (CLAUDE.md mục 9 — tách khỏi IpnHandler). */
@Service
public class SubscriptionManager {

    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    public SubscriptionManager(SubscriptionRepository subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    /**
     * Cấp quyền PRO sau khi thanh toán đã được xác thực.
     *
     * <p>Nếu người dùng đang còn hạn thì kỳ mới nối tiếp từ thời điểm hết hạn cũ chứ không tính
     * lại từ bây giờ — mua gia hạn sớm mà bị cắt mất phần còn lại của kỳ cũ là ăn chặn ngày người
     * dùng đã trả tiền.</p>
     *
     * @param userId        chủ sở hữu gói
     * @param plan          gói đã mua, quyết định số ngày hiệu lực
     * @param transactionId giao dịch sinh ra quyền này, để truy vết ngược
     * @return subscription vừa tạo
     */
    public Subscription activate(Long userId, Plan plan, UUID transactionId) {
        Instant now = clock.instant();
        Instant start = currentActive(userId, now)
                .map(Subscription::getEndAt)
                .orElse(now);

        return subscriptions.save(new Subscription(
                userId, plan.getCode(), start, start.plus(plan.getDurationDays(), ChronoUnit.DAYS), transactionId));
    }

    /**
     * Người dùng có đang PRO không, tính tại thời điểm gọi.
     *
     * <p>Trả về bản có hạn xa nhất khi lỡ tồn tại nhiều bản chồng nhau (mua trùng, sự cố dữ liệu):
     * chọn nhầm bản hết hạn sớm hơn sẽ cắt quyền của người đã trả tiền.</p>
     */
    public Optional<Subscription> currentActive(Long userId, Instant moment) {
        List<Subscription> active = subscriptions
                .findByUserIdAndStatusAndEndAtAfter(userId, Subscription.ACTIVE, moment);
        return active.stream()
                .filter(s -> s.isActiveAt(moment))
                .max(Comparator.comparing(Subscription::getEndAt));
    }

    /** Tiện cho tầng gọi: "user này có PRO không" tại thời điểm hiện tại. */
    public boolean isPro(Long userId) {
        return currentActive(userId, clock.instant()).isPresent();
    }
}
