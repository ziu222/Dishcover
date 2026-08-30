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
                        List.of("trung ga"), List.of("ca chua"), null, List.of()));
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

    @Test
    void candidateWithDietaryConflictsRendersWarningMarker() {
        List<RetrievedRecipe> candidates = List.of(
                new RetrievedRecipe("r1", "Phở bò", "pho-bo",
                        List.of(), List.of("Nước mắm", "Thăn bò"), null, List.of("Nước mắm")));

        String prompt = builder.build("Dị ứng: hải sản", candidates, List.of(), "cho tôi công thức phở bò");

        assertTrue(prompt.contains("⚠ LƯU Ý: chứa Nước mắm"));
    }

    @Test
    void candidateWithoutDietaryConflictsHasNoWarningMarker() {
        List<RetrievedRecipe> candidates = List.of(
                new RetrievedRecipe("r1", "Trứng chiên cà chua", "trung-chien-ca-chua",
                        List.of("trung ga"), List.of("ca chua"), null, List.of()));

        String prompt = builder.build("(không có thông tin)", candidates, List.of(), "nấu gì được?");

        // Rule 3 (văn bản tĩnh) LUÔN nhắc tới "⚠ LƯU Ý" như mô tả quy ước — chỉ kiểm tra marker
        // ĐỘNG gắn theo từng công thức cụ thể không xuất hiện khi không có xung đột.
        assertTrue(!prompt.contains("⚠ LƯU Ý: chứa"));
    }
}
