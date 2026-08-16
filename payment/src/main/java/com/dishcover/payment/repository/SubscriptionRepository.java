package com.dishcover.payment.repository;

import com.dishcover.payment.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/** Truy cập bảng subscription (quyền dùng gói PRO theo thời hạn). */
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Các gói còn hiệu lực của một người dùng tại thời điểm {@code moment} — dùng để trả lời câu
     * "user này có PRO không". Lọc luôn theo {@code endAt} thay vì chỉ đọc {@code status}: gói hết
     * hạn theo thời gian, không chờ job đổi cờ mới hết (khớp index idx_user_active).
     */
    List<Subscription> findByUserIdAndStatusAndEndAtAfter(Long userId, String status, Instant moment);
}
