package com.dishcover.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình VNPay nạp từ application.yml (khối {@code vnpay}).
 *
 * <p>{@code hashSecret} là secret thật — chỉ nạp qua biến môi trường, không bao giờ ghi giá trị
 * vào file cấu hình (CLAUDE.md mục 13). KHÔNG override {@code toString()} mặc định của record ở
 * đây bằng cách in đủ trường: record tự sinh {@code toString()} chứa mọi giá trị, nên lớp này
 * tuyệt đối không được đưa nguyên vào log.</p>
 *
 * @param tmnCode       mã website VNPay cấp (vnp_TmnCode)
 * @param hashSecret    chuỗi bí mật tạo checksum HMAC-SHA512 (vnp_HashSecret) — SECRET
 * @param payUrl        URL cổng thanh toán (sandbox hoặc production)
 * @param returnUrl     nơi trình duyệt quay về sau thanh toán; KHÔNG dùng để kích hoạt gói
 * @param version       vnp_Version, hiện là 2.1.0
 * @param command       vnp_Command, luôn là "pay" cho luồng thanh toán thường
 * @param currency      vnp_CurrCode, VND
 * @param locale        vnp_Locale, vn
 * @param expireMinutes số phút đơn hết hiệu lực, dùng cho vnp_ExpireDate và job quét đơn treo
 */
@ConfigurationProperties(prefix = "vnpay")
public record VnpayProperties(
        String tmnCode,
        String hashSecret,
        String payUrl,
        String returnUrl,
        String version,
        String command,
        String currency,
        String locale,
        int expireMinutes
) {
    /** Che secret khi cần in ra log/debug — không bao giờ log nguyên record này. */
    public String maskedHashSecret() {
        if (hashSecret == null || hashSecret.length() < 8) {
            return "***";
        }
        return hashSecret.substring(0, 4) + "***" + hashSecret.substring(hashSecret.length() - 4);
    }
}
