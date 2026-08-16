package com.dishcover.payment.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chuyển trạng thái giao dịch. {@code isPending()} là chốt chặn ở tầng code chống IPN gửi lặp ghi
 * đè đơn đã chốt (tầng DB còn ràng buộc UNIQUE (provider, provider_trans_id) nữa).
 */
class PaymentTransactionTest {

    private PaymentTransaction newTx() {
        return new PaymentTransaction(1L, "PRO_MONTHLY", 49000, "VNPAY");
    }

    @Test
    void donMoiTaoLaPENDING() {
        PaymentTransaction tx = newTx();

        assertEquals(PaymentTransaction.PENDING, tx.getStatus());
        assertTrue(tx.isPending());
    }

    @Test
    void chotThanhCongThiLuuMaGiaoDichCuaCong() {
        PaymentTransaction tx = newTx();
        tx.markSuccess("14023212");

        assertEquals(PaymentTransaction.SUCCESS, tx.getStatus());
        assertEquals("14023212", tx.getProviderTransId());
    }

    /** Đơn đã chốt không còn PENDING -> IPN lần 2 phải bị chặn trước khi kích hoạt gói lần nữa. */
    @Test
    void donDaChotThiKhongConPENDING() {
        PaymentTransaction tx = newTx();
        tx.markSuccess("14023212");

        assertFalse(tx.isPending());

        tx.markExpired();
        assertFalse(tx.isPending());
    }

    @Test
    void soTienChotTuLucTaoDon() {
        PaymentTransaction tx = newTx();

        assertEquals(49000, tx.getAmountVnd());
    }
}
