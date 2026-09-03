package com.dishcover.recipe.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test circuit breaker/fallback THẬT qua Spring AOP proxy (cùng pattern
 * matching/src/test/.../ClientResilienceTest, rag/src/test/.../ClientResilienceTest) — trỏ
 * services.*-url vào cổng không ai lắng nghe để ép lỗi kết nối thật, xác nhận cả 2 client index
 * (Giai đoạn B, CLAUDE.md mục 6) đều fail-open: index công thức không được chặn bởi RAG/Matching
 * tạm thời không khả dụng.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "services.rag-url=http://localhost:1",
        "services.matching-url=http://localhost:1"
})
class IndexClientResilienceTest {

    @Autowired
    RagIndexClient ragIndexClient;
    @Autowired
    MatchingIndexClient matchingIndexClient;

    @Test
    void embedFailureFallsBackToEmptyInsteadOfCrashing() {
        Optional<float[]> result = ragIndexClient.embed("Bearer x", "noi dung");
        assertTrue(result.isEmpty());
    }

    @Test
    void indexFailureFallsBackSilentlyInsteadOfCrashing() {
        assertDoesNotThrow(() -> matchingIndexClient.index(
                "Bearer x", "r1", "noi dung", new float[]{0.1f}, Map.of("name", "n")));
    }
}
