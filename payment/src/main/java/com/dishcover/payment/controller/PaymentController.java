package com.dishcover.payment.controller;

import com.dishcover.common.security.AuthenticatedUser;
import com.dishcover.payment.dto.PaymentDtos.CheckoutRequest;
import com.dishcover.payment.dto.PaymentDtos.CheckoutResponse;
import com.dishcover.payment.dto.PaymentDtos.PlanResponse;
import com.dishcover.payment.dto.PaymentDtos.TransactionResponse;
import com.dishcover.payment.repository.PlanRepository;
import com.dishcover.payment.service.CheckoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/** Endpoint thanh toán. userId luôn lấy từ JWT, không bao giờ nhận từ client. */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PlanRepository planRepository;
    private final CheckoutService checkoutService;

    public PaymentController(PlanRepository planRepository, CheckoutService checkoutService) {
        this.planRepository = planRepository;
        this.checkoutService = checkoutService;
    }

    /**
     * Bảng giá các gói còn bán. KHÔNG yêu cầu đăng nhập (khai báo ở SecurityConfig): người chưa
     * nâng cấp phải xem được giá mới quyết định mua, chặn ở đây là tự chặn đường bán hàng.
     *
     * @return danh sách gói, sắp xếp theo giá tăng dần
     */
    @GetMapping("/plans")
    public List<PlanResponse> plans() {
        return planRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparingInt(p -> p.getPriceVnd()))
                .map(PlanResponse::from)
                .toList();
    }

    /**
     * Tạo giao dịch và trả URL chuyển hướng sang cổng thanh toán.
     *
     * <p>Chỉ cần JWT hợp lệ, KHÔNG gắn {@code @RequiresPlan("PRO")}: đây chính là nơi người dùng
     * FREE bấm để mua gói PRO — gắn gate PRO vào đây thì chỉ ai đã có PRO mới mua được PRO.</p>
     *
     * @param me      người dùng đã xác thực, lấy từ JWT
     * @param req     mã gói muốn mua
     * @param request dùng để lấy IP người dùng cho cổng thanh toán
     * @return mã đơn + URL thanh toán, HTTP 201
     */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@AuthenticationPrincipal AuthenticatedUser me,
                                                     @Valid @RequestBody CheckoutRequest req,
                                                     HttpServletRequest request) {
        CheckoutResponse res = checkoutService.checkout(me.userId(), req.planCode(), clientIpOf(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /**
     * Tra cứu trạng thái đơn — client poll đây để biết kết quả thật, không tin tham số trên URL
     * redirect từ trình duyệt (CLAUDE.md mục 8 bước 6, mục 11).
     *
     * @param me      người dùng đã xác thực, lấy từ JWT
     * @param orderId mã đơn nhận được lúc checkout
     * @return trạng thái giao dịch
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu đơn không tồn tại hoặc không thuộc người này
     */
    @GetMapping("/transactions/{orderId}")
    public TransactionResponse transaction(@AuthenticationPrincipal AuthenticatedUser me,
                                            @PathVariable String orderId) {
        return checkoutService.getTransaction(me.userId(), orderId);
    }

    /**
     * IP người dùng để gửi sang cổng thanh toán. Ưu tiên {@code X-Forwarded-For} vì request đi qua
     * API Gateway — không có nó thì mọi giao dịch đều mang IP của Gateway. Chỉ lấy phần tử ĐẦU
     * (IP client gốc); header này do client gửi được nên chỉ dùng để báo cáo cho cổng thanh toán,
     * KHÔNG bao giờ dùng cho quyết định phân quyền.
     */
    private static String clientIpOf(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
