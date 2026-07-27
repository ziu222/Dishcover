package com.dishcover.rag.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Bật @Scheduled cho ConversationHistoryStore quét dọn TTL định kỳ (specs/rag-service.md mục 3.8). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
