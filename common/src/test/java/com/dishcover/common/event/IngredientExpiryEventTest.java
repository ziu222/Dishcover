package com.dishcover.common.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IngredientExpiryEventTest {

    @Test
    void topicConstantIsStable() {
        assertEquals("ingredient-expiry-events", IngredientExpiryEvent.TOPIC);
    }

    @Test
    void recordCarriesAllFields() {
        var event = new IngredientExpiryEvent(1L, 2L, "Cà chua", "ca chua",
                LocalDate.of(2026, 9, 10), "EXPIRING_SOON");
        assertEquals(1L, event.userId());
        assertEquals(2L, event.inventoryItemId());
        assertEquals("Cà chua", event.ingredientName());
        assertEquals("ca chua", event.normalizedName());
        assertEquals(LocalDate.of(2026, 9, 10), event.expiryDate());
        assertEquals("EXPIRING_SOON", event.status());
    }
}
