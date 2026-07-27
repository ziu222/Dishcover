package com.dishcover.rag.llm;

import com.dishcover.rag.pipeline.RetrievedRecipe;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder();

    @Test
    void emptyContextAndHistoryRenderPlaceholders() {
        String prompt = builder.build("(không có thông tin)", List.of(), List.of(), "nấu gì được?");

        assertTrue(prompt.contains("(không có công thức nào khớp trong hệ thống)"));
        assertTrue(prompt.contains("(chưa có)"));
        assertTrue(prompt.contains("nấu gì được?"));
        assertTrue(prompt.contains("CHỈ gợi ý các món trong danh sách CÔNG THỨC")); // template verbatim còn nguyên
    }

    @Test
    void nonEmptyContextAndHistoryRenderContent() {
        List<RetrievedRecipe> candidates = List.of(
                new RetrievedRecipe("r1", "Trứng chiên cà chua", "trung-chien-ca-chua",
                        List.of("trung ga"), List.of("ca chua"), null));
        List<ConversationTurn> history = List.of(
                new ConversationTurn("user", "tôi có trứng", Instant.now()),
                new ConversationTurn("assistant", "Bạn nấu trứng chiên nhé", Instant.now()));

        String prompt = builder.build("Dị ứng: hải sản", candidates, history, "còn thiếu gì?");

        assertTrue(prompt.contains("Trứng chiên cà chua"));
        assertTrue(prompt.contains("đã có: trung ga; cần thêm: ca chua"));
        assertTrue(prompt.contains("Người dùng: tôi có trứng"));
        assertTrue(prompt.contains("Trợ lý: Bạn nấu trứng chiên nhé"));
        assertTrue(prompt.contains("Dị ứng: hải sản"));
    }
}
