package com.dishcover.inventory.service;

import com.dishcover.common.event.IngredientExpiryEvent;
import com.dishcover.inventory.entity.UserIngredient;
import com.dishcover.inventory.repository.UserIngredientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientExpiryScannerTest {

    @Mock
    UserIngredientRepository repository;
    @Mock
    KafkaTemplate<String, IngredientExpiryEvent> kafkaTemplate;

    @Test
    void publishesOneEventPerCandidateWithDerivedStatus() {
        UserIngredient expired = new UserIngredient(1L, "Cà chua", "ca chua",
                BigDecimal.ONE, "quả", LocalDate.now().minusDays(1), "MANUAL", "FRESH");
        UserIngredient expiringSoon = new UserIngredient(1L, "Hành lá", "hanh la",
                BigDecimal.ONE, "nhánh", LocalDate.now().plusDays(2), "MANUAL", "FRESH");
        when(repository.findByStatusNotAndExpiryDateLessThanEqual("USED", LocalDate.now().plusDays(3)))
                .thenReturn(List.of(expired, expiringSoon));

        IngredientExpiryScanner scanner = new IngredientExpiryScanner(repository, kafkaTemplate);
        scanner.scan();

        ArgumentCaptor<IngredientExpiryEvent> captor = ArgumentCaptor.forClass(IngredientExpiryEvent.class);
        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), captor.capture());
        List<IngredientExpiryEvent> sent = captor.getAllValues();
        assertEquals("EXPIRED", sent.get(0).status());
        assertEquals("EXPIRING_SOON", sent.get(1).status());
    }

    @Test
    void noCandidatesMeansNoPublish() {
        when(repository.findByStatusNotAndExpiryDateLessThanEqual(anyString(), any(LocalDate.class)))
                .thenReturn(List.of());

        new IngredientExpiryScanner(repository, kafkaTemplate).scan();

        verify(kafkaTemplate, times(0)).send(anyString(), anyString(), any());
    }
}
