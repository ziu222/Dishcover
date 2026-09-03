package com.dishcover.notification.kafka;

import com.dishcover.common.event.IngredientExpiryEvent;
import com.dishcover.notification.client.UserClient;
import com.dishcover.notification.entity.Notification;
import com.dishcover.notification.mail.EmailSender;
import com.dishcover.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class IngredientExpiryListener {

    private final NotificationService notificationService;
    private final UserClient userClient;
    private final EmailSender emailSender;
    private final String frontendUrl;

    public IngredientExpiryListener(NotificationService notificationService,
                                     UserClient userClient,
                                     EmailSender emailSender,
                                     @Value("${app.frontend-url}") String frontendUrl) {
        this.notificationService = notificationService;
        this.userClient = userClient;
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
    }

    @KafkaListener(topics = IngredientExpiryEvent.TOPIC)
    public void onExpiryEvent(IngredientExpiryEvent event) {
        boolean expired = "EXPIRED".equals(event.status());
        String type = expired ? "INGREDIENT_EXPIRED" : "INGREDIENT_EXPIRING_SOON";
        String title = event.ingredientName() + (expired ? " đã hết hạn" : " sắp hết hạn");
        String message = "Hạn dùng: " + event.expiryDate();
        String actionUrl = "/goi-y?ingredient=" + event.normalizedName();

        Notification candidate = new Notification(event.userId(), type, title, message, actionUrl, event.inventoryItemId());
        notificationService.createIfAbsent(candidate).ifPresent(saved -> {
            String email = userClient.getEmail(event.userId());
            if (email != null) {
                emailSender.send(email, title, message, frontendUrl + actionUrl);
            }
        });
    }
}
