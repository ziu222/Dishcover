package com.dishcover.payment.controller;

import com.dishcover.payment.entity.Plan;
import com.dishcover.payment.repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bảng giá đi qua toàn bộ filter chain thật để chứng minh nó KHÔNG bị chặn bởi JWT. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    PlanRepository planRepository;

    /** Người chưa đăng nhập phải xem được giá, nếu không thì tự chặn đường bán hàng. */
    @Test
    void bangGiaXemDuocKhongCanDangNhap() throws Exception {
        when(planRepository.findByActiveTrue()).thenReturn(List.of(
                new Plan("PRO_YEARLY", 399000, 365),
                new Plan("PRO_MONTHLY", 49000, 30)));

        mvc.perform(get("/payments/plans"))
                .andExpect(status().isOk())
                // sắp xếp theo giá tăng dần, không theo thứ tự repository trả về
                .andExpect(jsonPath("$[0].code").value("PRO_MONTHLY"))
                .andExpect(jsonPath("$[0].priceVnd").value(49000))
                .andExpect(jsonPath("$[1].code").value("PRO_YEARLY"));
    }

    @Test
    void chiTraGoiConBan() throws Exception {
        when(planRepository.findByActiveTrue()).thenReturn(List.of(new Plan("PRO_MONTHLY", 49000, 30)));

        mvc.perform(get("/payments/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
