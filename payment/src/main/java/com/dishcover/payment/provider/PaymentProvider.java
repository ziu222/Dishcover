package com.dishcover.payment.provider;

import com.dishcover.payment.entity.PaymentTransaction;

import java.util.Map;

/**
 * Trừu tượng hóa cổng thanh toán (CLAUDE.md mục 8/9 "O") — thêm cổng mới là thêm một lớp
 * implement, KHÔNG sửa {@code CheckoutService} hay {@code IpnHandler}.
 *
 * <p>Đây là một trong số ít chỗ interface thật sự xứng đáng trong dự án này (đối chiếu mục 9 "I":
 * không tách interface cho HTTP client chỉ có một cài đặt): MoMo và VNPay là hai cài đặt có thật,
 * khác nhau cả thuật toán ký lẫn định dạng tham số, và cần thay được cho nhau tại thời điểm chạy.</p>
 */
public interface PaymentProvider {

    /** Mã cổng, khớp cột {@code provider} trong bảng giao dịch: {@code VNPAY} | {@code MOMO}. */
    String code();

    /**
     * Dựng URL để chuyển hướng người dùng sang cổng thanh toán.
     *
     * @param transaction đơn đã lưu ở trạng thái PENDING; id của nó dùng làm mã đơn gửi sang cổng
     * @param clientIp    IP người dùng, cổng thanh toán yêu cầu để chống gian lận
     * @return URL đầy đủ kèm chữ ký
     */
    String buildPaymentUrl(PaymentTransaction transaction, String clientIp);

    /**
     * Xác thực chữ ký của dữ liệu cổng thanh toán gửi về (IPN hoặc return URL).
     *
     * <p>Trả boolean thay vì ném exception: gọi xong còn phải đối chiếu tiếp số tiền và trạng thái
     * đơn, để bên gọi tự quyết định phản hồi thế nào cho từng loại sai.</p>
     */
    boolean verifyCallback(Map<String, String> params);

    /** Rút mã đơn (chính là id giao dịch phía mình) từ tham số callback. */
    String extractOrderId(Map<String, String> params);

    /** Rút mã giao dịch phía cổng thanh toán từ tham số callback. */
    String extractProviderTransId(Map<String, String> params);

    /** Rút số tiền (VND) từ tham số callback, để đối chiếu với đơn đã lưu. */
    long extractAmountVnd(Map<String, String> params);

    /** Cổng báo giao dịch thành công hay không. */
    boolean isSuccessful(Map<String, String> params);
}
