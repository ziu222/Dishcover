package com.dishcover.rag.client;

import com.dishcover.rag.exception.ApiExceptions.UpstreamUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
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

    /** Giai đoạn B — kênh vector search, fail-open giống searchByIngredients (mất kênh, còn 3 kênh kia). */
    @Test
    void vectorSearchFailureFallsBackToEmptyListInsteadOfCrashing() {
        List<VectorSearchDtos.VectorMatch> result =
                ragMatchingClient.vectorSearch("Bearer x", new float[]{0.1f, 0.2f}, 8);
        assertTrue(result.isEmpty());
    }

    /** Giai đoạn B — fetch chi tiết theo id cho kênh vector search, fail-open (null, bỏ qua id đó). */
    @Test
    void getByIdFailureFallsBackToNullInsteadOfCrashing() {
        assertNull(ragRecipeClient.getById("r7"));
    }

    @Test
    void userFailureFailsClosedInsteadOfAssumingNoAllergies() {
        assertThrows(UpstreamUnavailableException.class, () -> ragUserClient.getDietaryPreferences("Bearer x"));
    }
}
