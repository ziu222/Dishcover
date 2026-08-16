package com.dishcover.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Bật @Scheduled cho job quét đơn thanh toán treo quá hạn (CLAUDE.md mục 8). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
