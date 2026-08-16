package com.dishcover.payment.provider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Ký và xác thực chữ ký {@code vnp_SecureHash} của VNPay (HMAC-SHA512).
 *
 * <p>Đảm nhiệm vai trò {@code SignatureVerifier} trong CLAUDE.md mục 9. Cố ý gộp cả ký lẫn xác
 * thực vào MỘT lớp thay vì tách đôi: hai chiều bắt buộc phải dựng chuỗi dữ liệu theo đúng cùng
 * một quy tắc (thứ tự tham số, cách url-encode, tham số nào bị loại). Tách ra hai lớp nghĩa là
 * nhân đôi quy tắc đó ở hai nơi, và chỉ cần lệch một chi tiết là chữ ký mình tự ký lại không tự
 * xác thực được — đúng loại lỗi âm thầm mà việc gộp lại này loại bỏ hoàn toàn.</p>
 *
 * <p>Quy tắc dựng dữ liệu ký (theo tài liệu VNPay): loại {@code vnp_SecureHash} và
 * {@code vnp_SecureHashType}, bỏ tham số rỗng, sắp xếp tăng dần theo tên, url-encode, nối
 * {@code ten=gia_tri} bằng {@code &}, rồi HMAC-SHA512 với {@code vnp_HashSecret}, kết quả dạng hex.</p>
 */
public class VnpaySigner {

    private static final String HMAC_ALGORITHM = "HmacSHA512";

    /** Hai tham số này KHÔNG nằm trong dữ liệu ký — chính chúng là kết quả của phép ký. */
    private static final Set<String> EXCLUDED_FROM_HASH = Set.of("vnp_SecureHash", "vnp_SecureHashType");

    private final String hashSecret;

    /**
     * @param hashSecret chuỗi bí mật VNPay cấp (vnp_HashSecret) — nạp từ biến môi trường, không
     *                   bao giờ hardcode (CLAUDE.md mục 13)
     */
    public VnpaySigner(String hashSecret) {
        if (hashSecret == null || hashSecret.isBlank()) {
            throw new IllegalArgumentException("vnp_HashSecret rỗng — kiểm tra biến môi trường VNPAY_HASH_SECRET");
        }
        this.hashSecret = hashSecret;
    }

    /**
     * Dựng chuỗi dữ liệu đem đi ký/xác thực từ tập tham số.
     *
     * <p>{@link TreeMap} lo phần sắp xếp tăng dần theo tên — không phụ thuộc thứ tự tham số lúc
     * gọi vào, nên IPN của VNPay gửi tham số theo thứ tự nào cũng ra cùng một chuỗi.</p>
     */
    String buildHashData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(params).forEach((name, value) -> {
            if (value == null || value.isEmpty() || EXCLUDED_FROM_HASH.contains(name)) {
                return;
            }
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(encode(name)).append('=').append(encode(value));
        });
        return sb.toString();
    }

    /** @return chữ ký hex của tập tham số, dùng làm {@code vnp_SecureHash} */
    public String sign(Map<String, String> params) {
        return hmacSha512Hex(buildHashData(params));
    }

    /**
     * Xác thực chữ ký nhận từ VNPay (IPN hoặc return URL).
     *
     * <p>So sánh bằng {@link MessageDigest#isEqual} chứ KHÔNG dùng {@code equals}: so sánh chuỗi
     * thông thường thoát ra ngay tại ký tự đầu tiên khác nhau, thời gian chạy khác nhau theo số
     * ký tự khớp — kẻ tấn công đo thời gian phản hồi có thể dò dần ra chữ ký hợp lệ. Hàm này so
     * sánh trong thời gian không đổi.</p>
     *
     * @param params       toàn bộ tham số nhận được (kể cả vnp_SecureHash, sẽ tự bị loại khi ký lại)
     * @param receivedHash giá trị vnp_SecureHash nhận được
     * @return true nếu chữ ký hợp lệ
     */
    public boolean verify(Map<String, String> params, String receivedHash) {
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }
        String expected = sign(params);
        // VNPay trả hex; chuẩn hóa hoa/thường trước khi so để không phụ thuộc cách viết của họ.
        return MessageDigest.isEqual(
                expected.toLowerCase().getBytes(StandardCharsets.UTF_8),
                receivedHash.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Dựng query string để gắn vào URL thanh toán. Cùng quy tắc encode với dữ liệu ký, nhưng
     * GIỮ nguyên mọi tham số (không loại vnp_SecureHash) vì đây là phần gửi đi thật.
     */
    public String buildQuery(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(params).forEach((name, value) -> {
            if (value == null || value.isEmpty()) {
                return;
            }
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(encode(name)).append('=').append(encode(value));
        });
        return sb.toString();
    }

    /**
     * Dùng US_ASCII đúng như thư viện mẫu của VNPay — encode bằng bảng mã khác sẽ cho chuỗi khác
     * ở ký tự tiếng Việt (VD trong vnp_OrderInfo), dẫn tới chữ ký hai bên không khớp.
     */
    private static String encode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.US_ASCII);
    }

    private String hmacSha512Hex(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (java.security.GeneralSecurityException ex) {
            // HmacSHA512 là thuật toán bắt buộc có trong mọi JRE — lỗi ở đây là hỏng môi trường
            // chạy, không phải tình huống nghiệp vụ, nên không nuốt mà ném thẳng.
            throw new IllegalStateException("Không khởi tạo được HMAC-SHA512", ex);
        }
    }
}
