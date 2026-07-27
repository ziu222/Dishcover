package com.dishcover.matching.client;

import com.dishcover.matching.exception.ApiExceptions.UpstreamUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test circuit breaker/fallback THẬT qua Spring AOP proxy — khác MatchingServiceTest (new trực
 * tiếp, không qua Spring context nên @CircuitBreaker không có hiệu lực). Trỏ services.*-url vào
 * cổng không ai lắng nghe để ép lỗi kết nối thật, xác nhận đúng thiết kế fallback
 * (specs/matching-service.md mục 3.5 review): Inventory fail-open (rỗng, chỉ làm gợi ý kém đi),
 * Recipe/User fail-closed (UpstreamUnavailableException — không đoán bừa, nhất là dị ứng).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "services.inventory-url=http://localhost:1",
        "services.recipe-url=http://localhost:1",
        "services.user-url=http://localhost:1"
})
class ClientResilienceTest {

    @Autowired
    InventoryClient inventoryClient;
    @Autowired
    RecipeClient recipeClient;
    @Autowired
    UserClient userClient;

    @Test
    void inventoryFailureFallsBackToEmptyListInsteadOfCrashing() {
        List<InventoryItemDto> result = inventoryClient.getFreshItems("Bearer x");
        assertTrue(result.isEmpty());
    }

    @Test
    void recipeFailureThrowsUpstreamUnavailable() {
        assertThrows(UpstreamUnavailableException.class, recipeClient::getAllRecipesWithIngredients);
    }

    @Test
    void userFailureFailsClosedInsteadOfAssumingNoAllergies() {
        assertThrows(UpstreamUnavailableException.class, () -> userClient.getAllergenGroups("Bearer x"));
    }
}
