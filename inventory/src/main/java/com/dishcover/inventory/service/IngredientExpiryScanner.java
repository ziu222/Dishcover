package com.dishcover.inventory.service;

import com.dishcover.common.event.IngredientExpiryEvent;
import com.dishcover.inventory.entity.UserIngredient;
import com.dishcover.inventory.repository.UserIngredientRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Quét hàng ngày, publish {@link IngredientExpiryEvent} cho Notification Service (Kafka).
 * Không tự lọc "đã báo chưa" — cứ republish nguyên trạng mỗi ngày; dedup thuộc trách nhiệm phía
 * consumer (specs/notification-service.md mục 2). Mọi dòng repository trả về đều thoả
 * {@code status != USED AND expiryDate <= today + NEAR_EXPIRY_DAYS}, nên {@link
 * InventoryService#deriveStatus} luôn trả EXPIRED hoặc EXPIRING_SOON, không bao giờ FRESH/USED.
 */
@Component
public class IngredientExpiryScanner {

    private final UserIngredientRepository repository;
    private final KafkaTemplate<String, IngredientExpiryEvent> kafkaTemplate;

    public IngredientExpiryScanner(UserIngredientRepository repository,
                                    KafkaTemplate<String, IngredientExpiryEvent> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(cron = "${notification.scan-cron:0 0 7 * * *}")
    public void scan() {
        LocalDate threshold = LocalDate.now().plusDays(InventoryService.NEAR_EXPIRY_DAYS);
        for (UserIngredient item : repository.findByStatusNotAndExpiryDateLessThanEqual("USED", threshold)) {
            String status = InventoryService.deriveStatus(item.getStatus(), item.getExpiryDate());
            var event = new IngredientExpiryEvent(item.getUserId(), item.getId(), item.getIngredientName(),
                    item.getNormalizedName(), item.getExpiryDate(), status);
            kafkaTemplate.send(IngredientExpiryEvent.TOPIC, item.getUserId().toString(), event);
        }
    }
}
