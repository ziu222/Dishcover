package com.dishcover.notification.kafka;

import com.dishcover.common.event.IngredientExpiryEvent;
import com.dishcover.notification.client.UserClient;
import com.dishcover.notification.entity.Notification;
import com.dishcover.notification.mail.EmailSender;
import com.dishcover.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientExpiryListenerTest {

    @Mock
    NotificationService notificationService;
    @Mock
    UserClient userClient;
    @Mock
    EmailSender emailSender;

    IngredientExpiryListener listener() {
        return new IngredientExpiryListener(notificationService, userClient, emailSender, "http://localhost:5175");
    }

    @Test
    void newNotificationTriggersEmail() {
        var event = new IngredientExpiryEvent(1L, 2L, "Cà chua", "ca chua", LocalDate.now().plusDays(2), "EXPIRING_SOON");
        when(notificationService.createIfAbsent(any())).thenReturn(Optional.of(new Notification(
                1L, "INGREDIENT_EXPIRING_SOON", "x", "y", "/goi-y?ingredient=ca chua", 2L)));
        when(userClient.getEmail(1L)).thenReturn("user1@test.com");

        listener().onExpiryEvent(event);

        verify(emailSender, times(1)).send(eq("user1@test.com"), anyString(), anyString(), anyString());
    }

    @Test
    void duplicateEventDoesNotSendEmail() {
        var event = new IngredientExpiryEvent(1L, 2L, "Cà chua", "ca chua", LocalDate.now().plusDays(2), "EXPIRING_SOON");
        when(notificationService.createIfAbsent(any())).thenReturn(Optional.empty());

        listener().onExpiryEvent(event);

        verify(userClient, never()).getEmail(any());
        verify(emailSender, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void userClientFailureSkipsEmailWithoutThrowing() {
        var event = new IngredientExpiryEvent(1L, 2L, "Cà chua", "ca chua", LocalDate.now().plusDays(2), "EXPIRING_SOON");
        when(notificationService.createIfAbsent(any())).thenReturn(Optional.of(new Notification(
                1L, "INGREDIENT_EXPIRING_SOON", "x", "y", "/goi-y?ingredient=ca chua", 2L)));
        when(userClient.getEmail(1L)).thenReturn(null);

        listener().onExpiryEvent(event);

        verify(emailSender, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    /**
     * Hành vi HIỆN TẠI: listener không tự bắt lỗi ngoài DataIntegrityViolationException (đã xử lý
     * bên trong NotificationService.createIfAbsent) — lỗi khác (VD mất kết nối DB) bị ném thẳng lên
     * container Spring Kafka, để @{code DefaultErrorHandler} mặc định (retry rồi bỏ qua record,
     * KHÔNG lặp vô hạn) xử lý. Test này chỉ xác nhận listener KHÔNG nuốt lỗi này ở tầng của nó.
     */
    @Test
    void unexpectedCreateIfAbsentFailurePropagatesToKafkaContainer() {
        var event = new IngredientExpiryEvent(1L, 2L, "Cà chua", "ca chua", LocalDate.now().plusDays(2), "EXPIRING_SOON");
        when(notificationService.createIfAbsent(any())).thenThrow(new RuntimeException("DB connection lost"));

        assertThrows(RuntimeException.class, () -> listener().onExpiryEvent(event));

        verify(userClient, never()).getEmail(any());
        verify(emailSender, never()).send(anyString(), anyString(), anyString(), anyString());
    }
}
