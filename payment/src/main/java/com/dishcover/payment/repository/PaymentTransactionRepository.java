package com.dishcover.payment.repository;

import com.dishcover.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Truy cập bảng giao dịch thanh toán. */
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    /**
     * Dùng cho client poll kết quả (mục 8 bước 6) — luôn kèm userId để người dùng không tra được
     * đơn của người khác dù biết mã đơn.
     */
    Optional<PaymentTransaction> findByIdAndUserId(UUID id, Long userId);

    /** Job quét đơn treo quá lâu để chuyển sang EXPIRED (mục 8). */
    List<PaymentTransaction> findByStatusAndCreatedAtBefore(String status, Instant threshold);
}
