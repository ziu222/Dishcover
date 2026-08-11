package com.dishcover.rag.llm;

import com.dishcover.rag.pipeline.RetrievedRecipe;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Dựng prompt gửi LLM — template VERBATIM từ CLAUDE.md mục 6, KHÔNG được đổi 1 chữ
 * (specs/rag-service.md mục 3.4).
 */
@Component
public class PromptBuilder {

    private static final String TEMPLATE = """
            Bạn là trợ lý nấu ăn của hệ thống Leftover Recipe Matcher.
            QUY TẮC BẮT BUỘC:
            1. CHỈ gợi ý các món trong danh sách CÔNG THỨC dưới đây. Tuyệt đối không tự nghĩ ra món khác.
            2. Nếu không món nào phù hợp câu hỏi, nói thẳng là chưa có công thức phù hợp
               trong hệ thống và gợi ý người dùng thử nguyên liệu khác.
            3. Người dùng có đặc điểm ăn uống: %s. Không gợi ý món vi phạm.
            4. Trả lời tiếng Việt, thân thiện, ngắn gọn. Khi nhắc tên món, giữ đúng tên trong dữ liệu.

            CÔNG THỨC TRONG HỆ THỐNG: %s
            LỊCH SỬ HỘI THOẠI GẦN NHẤT: %s
            CÂU HỎI: %s
            """;

    /**
     * Điền {@link #TEMPLATE} với dữ liệu thật của lượt chat hiện tại.
     *
     * @param dietaryText mô tả dị ứng/chế độ ăn của người dùng (dạng text đã format sẵn)
     * @param candidates  danh sách công thức ứng viên từ {@code HybridRetriever}
     * @param history     các lượt hội thoại gần nhất (rỗng nếu chưa có/không theo dõi)
     * @param question    câu hỏi gốc của người dùng
     * @return prompt hoàn chỉnh gửi cho LLM
     */
    public String build(String dietaryText, List<RetrievedRecipe> candidates,
                         List<ConversationTurn> history, String question) {
        return TEMPLATE.formatted(dietaryText, formatContext(candidates), formatHistory(history), question);
    }

    private String formatContext(List<RetrievedRecipe> candidates) {
        if (candidates.isEmpty()) {
            return "(không có công thức nào khớp trong hệ thống)";
        }
        return IntStream.range(0, candidates.size())
                .mapToObj(i -> {
                    RetrievedRecipe r = candidates.get(i);
                    return "%d. %s (đã có: %s; cần thêm: %s)".formatted(i + 1, r.name(),
                            String.join(", ", r.matchedIngredients()), String.join(", ", r.missingIngredients()));
                })
                .collect(Collectors.joining("\n"));
    }

    private String formatHistory(List<ConversationTurn> history) {
        if (history.isEmpty()) {
            return "(chưa có)";
        }
        return history.stream()
                .map(t -> ("user".equals(t.role()) ? "Người dùng: " : "Trợ lý: ") + t.text())
                .collect(Collectors.joining("\n"));
    }
}
