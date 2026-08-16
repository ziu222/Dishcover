package com.dishcover.payment.provider;

import com.dishcover.payment.config.VnpayProperties;
import com.dishcover.payment.entity.PaymentTransaction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Dựng URL thanh toán và đọc ngược tham số callback. KHÔNG gọi VNPay thật. */
class VnpayProviderTest {

    private static final String SECRET = "test-hash-secret-khong-phai-that-000000";
    /** 2026-08-16 09:00:00 giờ Việt Nam. */
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-08-16T02:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    private final VnpayProperties props = new VnpayProperties(
            "TESTTMN1", SECRET, "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
            "http://localhost:5173/payment/result", "2.1.0", "pay", "VND", "vn", 15);
    private final VnpayProvider provider = new VnpayProvider(props, FIXED);
    private final VnpaySigner signer = new VnpaySigner(SECRET);

    private PaymentTransaction tx(int amountVnd) {
        PaymentTransaction t = new PaymentTransaction(7L, "PRO_MONTHLY", amountVnd, "VNPAY");
        setId(t, UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"));
        return t;
    }

    /** Id do JPA sinh lúc lưu; test không qua DB nên gán thẳng để có mã đơn xác định. */
    private void setId(PaymentTransaction t, UUID id) {
        try {
            Field f = PaymentTransaction.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(t, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Map<String, String> queryParams(String url) {
        Map<String, String> out = new HashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            int i = pair.indexOf('=');
            out.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        return out;
    }

    @Test
    void urlTroDungCongThanhToanSandbox() {
        String url = provider.buildPaymentUrl(tx(49000), "127.0.0.1");

        assertTrue(url.startsWith(props.payUrl() + "?"), url);
    }

    /** VNPay nhận vnp_Amount theo đơn vị nhỏ nhất — gửi thẳng số VND là thu thiếu 100 lần. */
    @Test
    void soTienNhan100KhiGuiSangVnpay() {
        Map<String, String> p = queryParams(provider.buildPaymentUrl(tx(49000), "127.0.0.1"));

        assertEquals("4900000", p.get("vnp_Amount"));
    }

    @Test
    void maDonChinhLaIdGiaoDich() {
        Map<String, String> p = queryParams(provider.buildPaymentUrl(tx(49000), "127.0.0.1"));

        assertEquals("0f8fad5b-d9cb-469f-a165-70867728950e", p.get("vnp_TxnRef"));
    }

    /** Mốc thời gian phải theo giờ Việt Nam, dùng UTC thì VNPay coi đơn hết hạn ngay. */
    @Test
    void mocThoiGianTheoGioVietNamVaHetHanDungCauHinh() {
        Map<String, String> p = queryParams(provider.buildPaymentUrl(tx(49000), "127.0.0.1"));

        assertEquals("20260816090000", p.get("vnp_CreateDate"));
        assertEquals("20260816091500", p.get("vnp_ExpireDate"));
    }

    /** URL tự dựng phải tự xác thực được — nếu không, VNPay sẽ từ chối ngay từ bước đầu. */
    @Test
    void urlDungRaTuXacThucDuoc() {
        Map<String, String> p = queryParams(provider.buildPaymentUrl(tx(49000), "127.0.0.1"));

        assertTrue(signer.verify(p, p.get("vnp_SecureHash")));
        assertTrue(provider.verifyCallback(p));
    }

    @Test
    void doiChieuDuocSoTienTuCallback() {
        Map<String, String> p = new HashMap<>();
        p.put("vnp_Amount", "4900000");

        assertEquals(49000L, provider.extractAmountVnd(p));
    }

    /**
     * Số tiền không đọc được trả -1 chứ không phải 0: bên gọi so sánh với số tiền đơn đã lưu,
     * 0 có thể trùng với gói dùng thử giá 0 nếu sau này thêm, -1 thì chắc chắn không khớp.
     */
    @Test
    void soTienHongThiTraGiaTriKhongTheKhopDon() {
        assertEquals(-1L, provider.extractAmountVnd(Map.of("vnp_Amount", "khong-phai-so")));
        assertEquals(-1L, provider.extractAmountVnd(Map.of()));
    }

    @Test
    void chiCoiLaThanhCongKhiCaHaiMaDeuLa00() {
        assertTrue(provider.isSuccessful(Map.of("vnp_ResponseCode", "00", "vnp_TransactionStatus", "00")));
        assertFalse(provider.isSuccessful(Map.of("vnp_ResponseCode", "00", "vnp_TransactionStatus", "02")));
        assertFalse(provider.isSuccessful(Map.of("vnp_ResponseCode", "24", "vnp_TransactionStatus", "00")));
        assertFalse(provider.isSuccessful(Map.of()));
    }

    @Test
    void maCongLaVNPAY() {
        assertEquals("VNPAY", provider.code());
    }
}
