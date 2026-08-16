package com.dishcover.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Đồng hồ dùng chung, tiêm được để test cố định thời điểm.
 *
 * <p>Thanh toán có hai chỗ phụ thuộc thời gian mà sai là hỏng thật: mốc {@code vnp_CreateDate}/
 * {@code vnp_ExpireDate} nằm trong dữ liệu ký, và job quét đơn treo quá hạn. Lấy thời gian qua
 * bean này thay vì gọi thẳng {@code Instant.now()} thì hai chỗ đó test được tất định.</p>
 *
 * <p>Dùng {@link Clock#systemUTC()}: bean chỉ cung cấp thời điểm, còn múi giờ do nơi hiển thị tự
 * quyết định — {@code VnpayProvider} tự quy đổi sang giờ Việt Nam khi định dạng cho VNPay.</p>
 */
@Configuration
public class TimeConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
