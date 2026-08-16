package com.dishcover.payment.controller;

import com.dishcover.common.security.JwtService;
import com.dishcover.payment.entity.PaymentTransaction;
import com.dishcover.payment.entity.Plan;
import com.dishcover.payment.provider.VnpaySigner;
import com.dishcover.payment.repository.PaymentTransactionRepository;
import com.dishcover.payment.repository.PlanRepository;
import com.dishcover.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chính sách xác thực của Payment Service qua toàn bộ filter chain thật.
 *
 * <p>Service này có chính sách KHÁC 5 service kia nên phải kiểm bằng HTTP thật chứ không suy luận
 * từ cấu hình: bảng giá và IPN công khai, checkout/tra cứu cần JWT, và tuyệt đối không có endpoint
 * nào cấp quyền PRO mà không qua chữ ký.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentSecurityTest {

    private static final String JWT_SECRET = "test-secret-at-least-32-chars-long-000";
    /** Trùng với vnpay.hash-secret trong application-test.yml. */
    private static final String VNPAY_SECRET = "test-hash-secret-khong-phai-that-000000";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    PlanRepository plans;
    @MockitoBean
    PaymentTransactionRepository transactions;
    @MockitoBean
    SubscriptionRepository subscriptions;

    private String token(String plan) {
        return "Bearer " + new JwtService(JWT_SECRET, 120).issue(7L, "buyer@test.com", plan);
    }

    // ---------- bảng giá: công khai ----------

    @Test
    void bangGiaCongKhai() throws Exception {
        when(plans.findByActiveTrue()).thenReturn(List.of(new Plan("PRO_MONTHLY", 49000, 30)));

        mvc.perform(get("/payments/plans")).andExpect(status().isOk());
    }

    // ---------- checkout: cần JWT, nhưng KHÔNG cần đã có PRO ----------

    private MockHttpServletRequestBuilder checkout() {
        return post("/payments/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planCode\":\"PRO_MONTHLY\"}");
    }

    @Test
    void checkoutKhongTokenThi401() throws Exception {
        mvc.perform(checkout()).andExpect(status().isUnauthorized());
    }

    /**
     * Người dùng FREE PHẢI mua được — đây chính là chỗ họ nâng cấp. Nếu chỗ này trả 402 thì chỉ
     * ai đã có PRO mới mua được PRO, tính năng trả phí thành bất khả mua.
     */
    @Test
    void nguoiDungFREEVanCheckoutDuoc() throws Exception {
        when(plans.findById("PRO_MONTHLY")).thenReturn(Optional.of(new Plan("PRO_MONTHLY", 49000, 30)));
        // JPA gán UUID lúc save(); mock phải làm y vậy, trả nguyên đối tượng chưa có id là sai
        // với hành vi thật chứ không phải lỗi mã nguồn.
        when(transactions.save(any())).thenAnswer(inv -> {
            PaymentTransaction t = inv.getArgument(0);
            Field f = PaymentTransaction.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(t, UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"));
            return t;
        });

        mvc.perform(checkout().header("Authorization", token("FREE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payUrl").exists());
    }

    // ---------- tra cứu giao dịch: cần JWT ----------

    @Test
    void traCuuGiaoDichKhongTokenThi401() throws Exception {
        mvc.perform(get("/payments/transactions/0f8fad5b-d9cb-469f-a165-70867728950e"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- IPN: công khai nhưng chặn bằng chữ ký ----------

    @Test
    void ipnKhongCanTokenNhungChuKyBiaThiTuChoi() throws Exception {
        mvc.perform(get("/payments/vnpay/ipn")
                        .param("vnp_TxnRef", "0f8fad5b-d9cb-469f-a165-70867728950e")
                        .param("vnp_Amount", "4900000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionStatus", "00")
                        .param("vnp_TransactionNo", "14023212")
                        .param("vnp_SecureHash", "chu-ky-bia-dat"))
                // Vào được endpoint (không 401) nhưng bị chữ ký chặn.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"));

        // Và tuyệt đối không được đụng tới DB khi chữ ký chưa hợp lệ.
        verify(transactions, never()).findById(any());
    }

    /**
     * Kịch bản tấn công đầy đủ qua HTTP: ký hợp lệ bằng secret ĐÚNG nhưng cho một đơn không tồn
     * tại. Chứng minh chữ ký hợp lệ vẫn chưa đủ để lấy PRO.
     */
    @Test
    void chuKyHopLeNhungDonKhongTonTaiThiVanKhongCapQuyen() throws Exception {
        when(transactions.findById(any())).thenReturn(Optional.empty());

        Map<String, String> params = new TreeMap<>(Map.of(
                "vnp_TxnRef", "0f8fad5b-d9cb-469f-a165-70867728950e",
                "vnp_Amount", "4900000",
                "vnp_ResponseCode", "00",
                "vnp_TransactionStatus", "00",
                "vnp_TransactionNo", "14023212"));
        String hash = new VnpaySigner(VNPAY_SECRET).sign(params);

        var req = get("/payments/vnpay/ipn").param("vnp_SecureHash", hash);
        params.forEach(req::param);

        mvc.perform(req)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("01"));

        verify(subscriptions, never()).save(any());
    }
}
