package com.dishcover.payment.controller;

import com.dishcover.payment.dto.PaymentDtos.PlanResponse;
import com.dishcover.payment.repository.PlanRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/** Endpoint thanh toán. Bảng giá công khai; các endpoint còn lại thêm dần ở step sau. */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PlanRepository planRepository;

    public PaymentController(PlanRepository planRepository) {
        this.planRepository = planRepository;
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
}
