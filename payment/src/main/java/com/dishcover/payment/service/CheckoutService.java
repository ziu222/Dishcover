package com.dishcover.payment.service;

import com.dishcover.common.exception.ResourceNotFoundException;
import com.dishcover.payment.dto.PaymentDtos.CheckoutResponse;
import com.dishcover.payment.dto.PaymentDtos.TransactionResponse;
import com.dishcover.payment.entity.Plan;
import com.dishcover.payment.entity.PaymentTransaction;
import com.dishcover.payment.provider.PaymentProvider;
import com.dishcover.payment.repository.PaymentTransactionRepository;
import com.dishcover.payment.repository.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Tạo giao dịch và tra cứu trạng thái (CLAUDE.md mục 8 bước 1-2 và 6).
 *
 * <p>Cố ý KHÔNG đụng tới việc kích hoạt gói: đó là việc của {@code IpnHandler} sau khi xác thực
 * chữ ký. Tách vậy để không có đường nào từ hành động của client dẫn thẳng tới quyền PRO.</p>
 */
@Service
public class CheckoutService {

    private final PlanRepository planRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentProvider paymentProvider;

    public CheckoutService(PlanRepository planRepository,
                           PaymentTransactionRepository transactionRepository,
                           PaymentProvider paymentProvider) {
        this.planRepository = planRepository;
        this.transactionRepository = transactionRepository;
        this.paymentProvider = paymentProvider;
    }

    /**
     * Tạo đơn PENDING rồi dựng URL thanh toán.
     *
     * <p>Số tiền lấy từ bảng giá phía server, KHÔNG nhận từ client (mục 8). Gói đã tắt bán cũng bị
     * từ chối chứ không chỉ gói không tồn tại — nếu không, mã gói cũ vẫn mua được mãi sau khi
     * ngừng bán.</p>
     *
     * @param userId   người dùng lấy từ JWT, không bao giờ nhận từ body
     * @param planCode mã gói client chọn
     * @param clientIp IP người dùng, cổng thanh toán yêu cầu
     * @return mã đơn + URL chuyển hướng sang cổng thanh toán
     * @throws ResourceNotFoundException nếu gói không tồn tại hoặc đã ngừng bán
     */
    @Transactional
    public CheckoutResponse checkout(Long userId, String planCode, String clientIp) {
        Plan plan = planRepository.findById(planCode)
                .filter(Plan::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Gói không tồn tại hoặc đã ngừng bán: " + planCode));

        PaymentTransaction tx = transactionRepository.save(
                new PaymentTransaction(userId, plan.getCode(), plan.getPriceVnd(), paymentProvider.code()));

        return new CheckoutResponse(tx.getId().toString(), paymentProvider.buildPaymentUrl(tx, clientIp));
    }

    /**
     * Tra cứu trạng thái đơn — client poll hàm này để biết kết quả thật (mục 8 bước 6).
     *
     * <p>Luôn lọc theo cả {@code userId}: biết mã đơn của người khác cũng không xem được, và
     * người ngoài không dò được đơn nào tồn tại. Không tìm thấy trả 404 (không phải 403) để không
     * tiết lộ đơn đó có tồn tại hay không.</p>
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long userId, String orderId) {
        UUID id;
        try {
            id = UUID.fromString(orderId);
        } catch (IllegalArgumentException ex) {
            // Mã đơn sai định dạng: trả 404 giống hệt trường hợp không tồn tại, không phải 400 —
            // phản hồi khác nhau sẽ giúp người dò biết định dạng nào là hợp lệ.
            throw new ResourceNotFoundException("Không tìm thấy giao dịch: " + orderId);
        }
        return transactionRepository.findByIdAndUserId(id, userId)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + orderId));
    }
}
