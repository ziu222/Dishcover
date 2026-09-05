package com.dishcover.inventory.repository;

import com.dishcover.inventory.entity.UserIngredient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Query thật (H2) dùng bởi {@code IngredientExpiryScanner} — IngredientExpiryScannerTest chỉ mock
 * repository, chưa xác nhận biên NULL/USED/ngày ngưỡng của chính câu query.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserIngredientRepositoryExpiryQueryTest {

    @Autowired
    UserIngredientRepository repository;
    @Autowired
    TestEntityManager em;

    private static final LocalDate THRESHOLD = LocalDate.now().plusDays(3);

    @Test
    void excludesUsedStatusEvenWithinThreshold() {
        UserIngredient used = new UserIngredient(1L, "Cà chua", "ca chua",
                BigDecimal.ONE, "quả", LocalDate.now().plusDays(1), "MANUAL", "USED");
        em.persistAndFlush(used);

        List<UserIngredient> result = repository.findByStatusNotAndExpiryDateLessThanEqual("USED", THRESHOLD);

        assertTrue(result.isEmpty());
    }

    @Test
    void excludesNullExpiryDate() {
        UserIngredient noExpiry = new UserIngredient(1L, "Muối", "muoi",
                BigDecimal.ONE, "gói", null, "MANUAL", "FRESH");
        em.persistAndFlush(noExpiry);

        List<UserIngredient> result = repository.findByStatusNotAndExpiryDateLessThanEqual("USED", THRESHOLD);

        assertTrue(result.isEmpty());
    }

    @Test
    void includesItemExactlyAtThresholdDate() {
        UserIngredient atThreshold = new UserIngredient(1L, "Hành lá", "hanh la",
                BigDecimal.ONE, "nhánh", THRESHOLD, "MANUAL", "FRESH");
        em.persistAndFlush(atThreshold);

        List<UserIngredient> result = repository.findByStatusNotAndExpiryDateLessThanEqual("USED", THRESHOLD);

        assertEquals(1, result.size());
        assertEquals("hanh la", result.get(0).getNormalizedName());
    }

    @Test
    void excludesItemPastThresholdDate() {
        UserIngredient farFuture = new UserIngredient(1L, "Gạo", "gao",
                BigDecimal.ONE, "kg", THRESHOLD.plusDays(1), "MANUAL", "FRESH");
        em.persistAndFlush(farFuture);

        List<UserIngredient> result = repository.findByStatusNotAndExpiryDateLessThanEqual("USED", THRESHOLD);

        assertTrue(result.isEmpty());
    }
}
