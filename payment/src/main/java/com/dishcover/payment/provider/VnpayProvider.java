package com.dishcover.payment.provider;

import com.dishcover.payment.config.VnpayProperties;
import com.dishcover.payment.entity.PaymentTransaction;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cài đặt {@link PaymentProvider} cho VNPay (API 2.1.0).
 *
 * <p>Hai quy ước của VNPay dễ sai nếu không đọc kỹ tài liệu, nên chốt ở đây một lần:</p>
 * <ul>
 *   <li><b>Số tiền nhân 100</b> — VNPay nhận {@code vnp_Amount} theo đơn vị nhỏ nhất. Gửi thẳng
 *       số VND sẽ thu thiếu đúng 100 lần, và chiều ngược lại phải chia 100 khi đối chiếu.</li>
 *   <li><b>Mốc thời gian theo giờ Việt Nam</b> ({@code Asia/Ho_Chi_Minh}, định dạng
 *       {@code yyyyMMddHHmmss}) — dùng giờ UTC của máy chủ thì đơn bị VNPay coi là hết hạn ngay.</li>
 * </ul>
 */
@Component
public class VnpayProvider implements PaymentProvider {

    public static final String CODE = "VNPAY";

    /** VNPay báo thành công bằng mã "00"; mọi mã khác là thất bại/hủy. */
    private static final String RESPONSE_SUCCESS = "00";

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties props;
    private final VnpaySigner signer;
    private final Clock clock;

    /**
     * Chỉ MỘT constructor — có hai constructor (một cho Spring, một để test tiêm {@link Clock})
     * thì Spring không biết chọn cái nào và quay ra tìm constructor rỗng, gây lỗi khởi tạo bean.
     * {@code Clock} lấy từ {@code TimeConfig} vì mốc thời gian nằm trong dữ liệu ký, phải test
     * được tất định.
     */
    public VnpayProvider(VnpayProperties props, Clock clock) {
        this.props = props;
        this.signer = new VnpaySigner(props.hashSecret());
        this.clock = clock;
    }

    /** {@inheritDoc} */
    @Override
    public String code() {
        return CODE;
    }

    /** {@inheritDoc} */
    @Override
    public String buildPaymentUrl(PaymentTransaction transaction, String clientIp) {
        var now = clock.instant().atZone(VN_ZONE);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", props.version());
        params.put("vnp_Command", props.command());
        params.put("vnp_TmnCode", props.tmnCode());
        params.put("vnp_Amount", String.valueOf(transaction.getAmountVnd() * 100L));
        params.put("vnp_CurrCode", props.currency());
        params.put("vnp_TxnRef", transaction.getId().toString());
        params.put("vnp_OrderInfo", "Nang cap goi " + transaction.getPlanCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", props.locale());
        params.put("vnp_ReturnUrl", props.returnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", VNPAY_TIME.format(now));
        params.put("vnp_ExpireDate", VNPAY_TIME.format(now.plusMinutes(props.expireMinutes())));

        params.put("vnp_SecureHash", signer.sign(params));
        return props.payUrl() + "?" + signer.buildQuery(params);
    }

    /** {@inheritDoc} */
    @Override
    public boolean verifyCallback(Map<String, String> params) {
        return signer.verify(params, params.get("vnp_SecureHash"));
    }

    /** {@inheritDoc} */
    @Override
    public String extractOrderId(Map<String, String> params) {
        return params.get("vnp_TxnRef");
    }

    /** {@inheritDoc} */
    @Override
    public String extractProviderTransId(Map<String, String> params) {
        return params.get("vnp_TransactionNo");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Chia 100 để trả về đúng số VND (xem ghi chú đơn vị ở đầu lớp). Giá trị không đọc được
     * trả -1 thay vì ném lỗi hay trả 0: bên gọi so sánh với số tiền đơn đã lưu, -1 chắc chắn
     * không khớp nên tự động bị từ chối, còn 0 thì trùng với đơn giá 0 nếu sau này có gói dùng thử.</p>
     */
    @Override
    public long extractAmountVnd(Map<String, String> params) {
        try {
            return Long.parseLong(params.getOrDefault("vnp_Amount", "-100")) / 100L;
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSuccessful(Map<String, String> params) {
        return RESPONSE_SUCCESS.equals(params.get("vnp_ResponseCode"))
                && RESPONSE_SUCCESS.equals(params.get("vnp_TransactionStatus"));
    }
}
