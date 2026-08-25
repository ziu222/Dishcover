package com.dishcover.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Bật @Scheduled cho LoginAttemptTracker quét dọn TTL định kỳ. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
