package com.dishcover.payment.provider;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chữ ký là ranh giới tin cậy duy nhất của luồng thanh toán — sai ở đây thì hoặc không ai thanh
 * toán được, hoặc ai cũng tự kích hoạt được gói PRO. Vì vậy test cả bằng vector đối chiếu chéo
 * chứ không chỉ round-trip: round-trip vẫn xanh dù cách dựng chuỗi ký sai hoàn toàn so với VNPay,
 * miễn là mình sai nhất quán ở cả hai chiều.
 */
class VnpaySignerTest {

    private static final String SECRET = "test-hash-secret-khong-phai-that-000000";

    /**
     * Chữ ký kỳ vọng tính bằng một cài đặt HMAC-SHA512 ĐỘC LẬP (Node.js crypto) trên cùng bộ tham
     * số, với bản sao chính xác quy tắc encode của {@code java.net.URLEncoder}. Nếu cách dựng
     * chuỗi ký ở {@link VnpaySigner} lệch khỏi tài liệu VNPay (thứ tự, encode, tham số bị loại),
     * test này đỏ ngay — điều mà test round-trip không phát hiện được.
     */
    private static final String EXPECTED_HASH =
            "2df551cfcd2d9ad2af873275d95aba49fc8e3f19c1c15c2bf04db0f7345d98da"
                    + "00362c66efe8deb5c6b60898ad8e2ed7f14860fe67feaac7a2284cd0562f2842";

    private final VnpaySigner signer = new VnpaySigner(SECRET);

    /** Bộ tham số mẫu, cố ý gồm cả khoảng trắng (encode thành '+') và ký tự cần escape trong URL. */
    private Map<String, String> sampleParams() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", "TESTTMN1");
        p.put("vnp_Amount", "4900000");
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", "0f8fad5b-d9cb-469f-a165-70867728950e");
        p.put("vnp_OrderInfo", "Nang cap goi PRO");
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_ReturnUrl", "http://localhost:5173/payment/result");
        p.put("vnp_IpAddr", "127.0.0.1");
        p.put("vnp_CreateDate", "20260816090000");
        return p;
    }

    @Test
    void chuKyKhopVoiCaiDatDocLap() {
        assertEquals(EXPECTED_HASH, signer.sign(sampleParams()));
    }

    @Test
    void loaiVnpSecureHashVaSecureHashTypeKhoiDuLieuKy() {
        Map<String, String> withHash = sampleParams();
        withHash.put("vnp_SecureHash", "GIA-TRI-BAT-KY");
        withHash.put("vnp_SecureHashType", "HMACSHA512");

        // Có hay không hai tham số đó thì chữ ký vẫn phải như nhau.
        assertEquals(EXPECTED_HASH, signer.sign(withHash));
    }

    @Test
    void boQuaThamSoRong() {
        Map<String, String> withEmpty = sampleParams();
        withEmpty.put("vnp_BankCode", "");

        assertEquals(EXPECTED_HASH, signer.sign(withEmpty));
    }

    /** IPN của VNPay gửi tham số theo thứ tự tùy ý — chữ ký không được phụ thuộc thứ tự đó. */
    @Test
    void khongPhuThuocThuTuThamSo() {
        Map<String, String> shuffled = new HashMap<>(sampleParams());

        assertEquals(EXPECTED_HASH, signer.sign(shuffled));
    }

    @Test
    void tuKyThiTuXacThucDuoc() {
        Map<String, String> p = sampleParams();

        assertTrue(signer.verify(p, signer.sign(p)));
    }

    /** Kịch bản tấn công thật: sửa số tiền rồi giữ nguyên chữ ký cũ. */
    @Test
    void suaSoTienThiChuKyKhongConHopLe() {
        Map<String, String> p = sampleParams();
        String hash = signer.sign(p);

        p.put("vnp_Amount", "100");

        assertFalse(signer.verify(p, hash));
    }

    @Test
    void chuKyBiaThiTuChoi() {
        Map<String, String> p = sampleParams();

        assertFalse(signer.verify(p, "deadbeef"));
        assertFalse(signer.verify(p, null));
        assertFalse(signer.verify(p, "   "));
    }

    /** Secret khác thì không xác thực được — chốt chặn với kẻ không biết vnp_HashSecret. */
    @Test
    void secretKhacThiKhongXacThucDuoc() {
        Map<String, String> p = sampleParams();
        String hashCuaKeGiaMao = new VnpaySigner("secret-khac-hoan-toan-0000000000000000").sign(p);

        assertFalse(signer.verify(p, hashCuaKeGiaMao));
    }

    /** VNPay có thể trả hex viết hoa — không được vì thế mà từ chối chữ ký hợp lệ. */
    @Test
    void chapNhanHexVietHoa() {
        Map<String, String> p = sampleParams();

        assertTrue(signer.verify(p, signer.sign(p).toUpperCase()));
    }

    @Test
    void queryStringGiuLaiVnpSecureHash() {
        Map<String, String> p = sampleParams();
        p.put("vnp_SecureHash", "abc123");

        String query = signer.buildQuery(p);

        assertTrue(query.contains("vnp_SecureHash=abc123"), "query gửi đi phải mang chữ ký: " + query);
    }

    /** Thiếu cấu hình secret phải chết ngay lúc khởi tạo, không âm thầm ký bằng chuỗi rỗng. */
    @Test
    void secretRongThiTuChoiKhoiTao() {
        assertThrows(IllegalArgumentException.class, () -> new VnpaySigner(""));
        assertThrows(IllegalArgumentException.class, () -> new VnpaySigner(null));
    }
}
