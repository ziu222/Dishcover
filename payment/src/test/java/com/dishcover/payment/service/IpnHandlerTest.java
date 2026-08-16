package com.dishcover.payment.service;

import com.dishcover.payment.entity.PaymentTransaction;
import com.dishcover.payment.entity.Plan;
import com.dishcover.payment.provider.PaymentProvider;
import com.dishcover.payment.repository.PaymentTransactionRepository;
import com.dishcover.payment.repository.PlanRepository;
import com.dishcover.payment.service.IpnHandler.IpnResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IPN là đường DUY NHẤT kích hoạt gói PRO — mọi kịch bản dưới đây đều là cách người ta sẽ thử
 * lấy PRO miễn phí. Test tập trung vào việc CHẶN, không chỉ vào luồng thuận.
 */
class IpnHandlerTest {

    private static final UUID ORDER_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

    private PaymentTransactionRepository transactions;
    private PlanRepository plans;
    private PaymentProvider provider;
    private SubscriptionManager subscriptions;
    private IpnHandler handler;

    private final Map<String, String> params = Map.of(
            "vnp_TxnRef", ORDER_ID.toString(), "vnp_Amount", "4900000",
            "vnp_ResponseCode", "00", "vnp_TransactionNo", "14023212", "vnp_SecureHash", "abc");

    @BeforeEach
    void setUp() {
        transactions = mock(PaymentTransactionRepository.class);
        plans = mock(PlanRepository.class);
        provider = mock(PaymentProvider.class);
        subscriptions = mock(SubscriptionManager.class);
        handler = new IpnHandler(transactions, plans, provider, subscriptions);

        // Mặc định: chữ ký hợp lệ, đúng đơn, đúng tiền, cổng báo thành công.
        when(provider.verifyCallback(any())).thenReturn(true);
        when(provider.extractOrderId(any())).thenReturn(ORDER_ID.toString());
        when(provider.extractAmountVnd(any())).thenReturn(49000L);
        when(provider.extractProviderTransId(any())).thenReturn("14023212");
        when(provider.isSuccessful(any())).thenReturn(true);
        when(plans.findById("PRO_MONTHLY")).thenReturn(Optional.of(new Plan("PRO_MONTHLY", 49000, 30)));
    }

    private PaymentTransaction pendingTx() {
        PaymentTransaction t = new PaymentTransaction(7L, "PRO_MONTHLY", 49000, "VNPAY");
        try {
            Field f = PaymentTransaction.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(t, ORDER_ID);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return t;
    }

    @Test
    void thanhToanHopLeThiKichHoatGoi() {
        when(transactions.findById(ORDER_ID)).thenReturn(Optional.of(pendingTx()));

        IpnResult res = handler.handle(params);

        assertEquals("00", res.rspCode());
        verify(subscriptions).activate(any(), any(), any());
    }

    /** Kịch bản tấn công 1: tự gọi IPN mà không biết vnp_HashSecret. */
    @Test
    void chuKySaiThiTuChoiVaKhongDocDenDon() {
        when(provider.verifyCallback(any())).thenReturn(false);

        IpnResult res = handler.handle(params);

        assertEquals("97", res.rspCode());
        // Chưa verify thì tuyệt đối không được dùng tham số đó để truy vấn DB.
        verify(transactions, never()).findById(any());
        verify(subscriptions, never()).activate(any(), any(), any());
    }

    /** Kịch bản tấn công 2: chữ ký hợp lệ của đơn 1.000đ nhưng khai là đã trả 399.000đ. */
    @Test
    void soTienLechThiTuChoiVaKhongKichHoat() {
        when(transactions.findById(ORDER_ID)).thenReturn(Optional.of(pendingTx()));
        when(provider.extractAmountVnd(any())).thenReturn(1000L);

        IpnResult res = handler.handle(params);

        assertEquals("04", res.rspCode());
        verify(subscriptions, never()).activate(any(), any(), any());
    }

    /** Kịch bản tấn công 3: phát lại (replay) đúng IPN thật nhiều lần để cộng dồn thời hạn. */
    @Test
    void ipnLapLaiKhongKichHoatThemLanNua() {
        PaymentTransaction daChot = pendingTx();
        daChot.markSuccess("14023212");
        when(transactions.findById(ORDER_ID)).thenReturn(Optional.of(daChot));

        IpnResult res = handler.handle(params);

        assertEquals("02", res.rspCode());
        verify(subscriptions, never()).activate(any(), any(), any());
    }

    @Test
    void donKhongTonTaiThiBao01() {
        when(transactions.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertEquals("01", handler.handle(params).rspCode());
        verify(subscriptions, never()).activate(any(), any(), any());
    }

    @Test
    void maDonSaiDinhDangCungBao01() {
        when(provider.extractOrderId(any())).thenReturn("khong-phai-uuid");

        assertEquals("01", handler.handle(params).rspCode());
        verify(subscriptions, never()).activate(any(), any(), any());
    }

    /**
     * Cổng báo thất bại: đánh dấu đơn FAILED, KHÔNG cấp quyền, nhưng vẫn trả 00 vì mình đã ghi
     * nhận được thông báo — trả mã khác sẽ khiến VNPay gửi lại mãi.
     */
    @Test
    void congBaoThatBaiThiDanhDauFailedNhungVanXacNhanDaNhan() {
        PaymentTransaction tx = pendingTx();
        when(transactions.findById(ORDER_ID)).thenReturn(Optional.of(tx));
        when(provider.isSuccessful(any())).thenReturn(false);

        IpnResult res = handler.handle(params);

        assertEquals("00", res.rspCode());
        assertEquals(PaymentTransaction.FAILED, tx.getStatus());
        verify(subscriptions, never()).activate(any(), any(), any());
    }

    /** Thứ tự kiểm tra: sai chữ ký phải bị chặn TRƯỚC cả khi số tiền cũng sai. */
    @Test
    void chuKyDuocKiemTraTruocSoTien() {
        when(provider.verifyCallback(any())).thenReturn(false);
        when(provider.extractAmountVnd(any())).thenReturn(1L);

        assertEquals("97", handler.handle(params).rspCode());
    }

    @Test
    void kichHoatDungGoiVaDungNguoiDung() {
        when(transactions.findById(ORDER_ID)).thenReturn(Optional.of(pendingTx()));

        handler.handle(params);

        verify(subscriptions).activate(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.argThat(p -> "PRO_MONTHLY".equals(p.getCode())),
                org.mockito.ArgumentMatchers.eq(ORDER_ID));
    }
}
