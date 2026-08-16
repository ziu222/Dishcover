package com.dishcover.payment.service;

import com.dishcover.common.exception.ResourceNotFoundException;
import com.dishcover.payment.dto.PaymentDtos.CheckoutResponse;
import com.dishcover.payment.entity.PaymentTransaction;
import com.dishcover.payment.entity.Plan;
import com.dishcover.payment.provider.PaymentProvider;
import com.dishcover.payment.repository.PaymentTransactionRepository;
import com.dishcover.payment.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tạo đơn và tra cứu — collaborator mock, không đụng DB lẫn cổng thanh toán. */
class CheckoutServiceTest {

    private static final UUID ORDER_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

    private PlanRepository plans;
    private PaymentTransactionRepository transactions;
    private PaymentProvider provider;
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        plans = mock(PlanRepository.class);
        transactions = mock(PaymentTransactionRepository.class);
        provider = mock(PaymentProvider.class);
        when(provider.code()).thenReturn("VNPAY");
        service = new CheckoutService(plans, transactions, provider);
    }

    private PaymentTransaction saved(int amountVnd) {
        PaymentTransaction t = new PaymentTransaction(7L, "PRO_MONTHLY", amountVnd, "VNPAY");
        try {
            Field f = PaymentTransaction.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(t, ORDER_ID);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return t;
    }

    /** Giá lấy từ bảng giá server — kể cả client có tìm cách gửi số tiền khác cũng vô hiệu. */
    @Test
    void soTienLayTuBangGiaPhiaServer() {
        when(plans.findById("PRO_MONTHLY")).thenReturn(Optional.of(new Plan("PRO_MONTHLY", 49000, 30)));
        when(transactions.save(any())).thenAnswer(inv -> {
            PaymentTransaction t = inv.getArgument(0);
            assertEquals(49000, t.getAmountVnd());
            return saved(49000);
        });
        when(provider.buildPaymentUrl(any(), anyString())).thenReturn("https://sandbox/pay?x=1");

        CheckoutResponse res = service.checkout(7L, "PRO_MONTHLY", "127.0.0.1");

        assertEquals(ORDER_ID.toString(), res.orderId());
        assertEquals("https://sandbox/pay?x=1", res.payUrl());
    }

    @Test
    void donMoiTaoLuonOTrangThaiPENDING() {
        when(plans.findById("PRO_MONTHLY")).thenReturn(Optional.of(new Plan("PRO_MONTHLY", 49000, 30)));
        when(transactions.save(any())).thenAnswer(inv -> {
            PaymentTransaction t = inv.getArgument(0);
            assertEquals(PaymentTransaction.PENDING, t.getStatus());
            return saved(49000);
        });
        when(provider.buildPaymentUrl(any(), anyString())).thenReturn("https://sandbox/pay");

        service.checkout(7L, "PRO_MONTHLY", "127.0.0.1");
    }

    @Test
    void goiKhongTonTaiThiKhongTaoDon() {
        when(plans.findById("KHONG_CO")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.checkout(7L, "KHONG_CO", "127.0.0.1"));
        verify(transactions, never()).save(any());
    }

    /** Gói đã tắt bán cũng phải bị từ chối, nếu không mã gói cũ vẫn mua được mãi sau khi ngừng bán. */
    @Test
    void goiDaNgungBanThiTuChoi() {
        Plan ngungBan = new Plan("PRO_MONTHLY", 49000, 30);
        ngungBan.setActive(false);
        when(plans.findById("PRO_MONTHLY")).thenReturn(Optional.of(ngungBan));

        assertThrows(ResourceNotFoundException.class, () -> service.checkout(7L, "PRO_MONTHLY", "127.0.0.1"));
        verify(transactions, never()).save(any());
    }

    /** Biết mã đơn người khác cũng không xem được — truy vấn luôn kèm userId. */
    @Test
    void khongTraDonCuaNguoiKhac() {
        when(transactions.findByIdAndUserId(ORDER_ID, 99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getTransaction(99L, ORDER_ID.toString()));
    }

    /**
     * Mã đơn sai định dạng trả 404 giống hệt trường hợp không tồn tại, KHÔNG phải 400: phản hồi
     * khác nhau sẽ giúp người đang dò biết định dạng nào là hợp lệ.
     */
    @Test
    void maDonSaiDinhDangCungTra404() {
        assertThrows(ResourceNotFoundException.class, () -> service.getTransaction(7L, "khong-phai-uuid"));
    }

    @Test
    void traDungTrangThaiDonCuaChinhMinh() {
        when(transactions.findByIdAndUserId(ORDER_ID, 7L)).thenReturn(Optional.of(saved(49000)));

        var res = service.getTransaction(7L, ORDER_ID.toString());

        assertEquals(PaymentTransaction.PENDING, res.status());
        assertEquals(49000, res.amountVnd());
    }
}
