package com.dishcover.rag.client;

import com.dishcover.rag.exception.ApiExceptions.UpstreamUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test circuit breaker/fallback THẬT qua Spring AOP proxy (copy pattern
 * matching/src/test/java/com/dishcover/matching/client/ClientResilienceTest.java) — trỏ
 * services.*-url vào cổng không ai lắng nghe để ép lỗi kết nối thật, xác nhận đúng thiết kế
 * fallback (specs/rag-service.md mục 3.3/5): Matching fail-open (rỗng), User fail-closed
 * (UpstreamUnavailableException — an toàn dị ứng).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "services.matching-url=http://localhost:1",
        "services.user-url=http://localhost:1",
        "services.recipe-url=http://localhost:1"
})
class ClientResilienceTest {

    @Autowired
    RagMatchingClient ragMatchingClient;
    @Autowired
    RagUserClient ragUserClient;
    @Autowired
    RagRecipeClient ragRecipeClient;

    @Test
    void matchingFailureFallsBackToEmptyListInsteadOfCrashing() {
        List<RecipeMatchDto> result = ragMatchingClient.searchByIngredients("Bearer x", List.of("trung ga"), 5);
        assertTrue(result.isEmpty());
    }

    @Test
    void recipeNameSearchFailureFallsBackToEmptyListInsteadOfCrashing() {
        List<RecipeDetailDto> result = ragRecipeClient.searchByName("Phở bò");
        assertTrue(result.isEmpty());
    }

    @Test
    void userFailureFailsClosedInsteadOfAssumingNoAllergies() {
        assertThrows(UpstreamUnavailableException.class, () -> ragUserClient.getDietaryPreferences("Bearer x"));
    }
}
