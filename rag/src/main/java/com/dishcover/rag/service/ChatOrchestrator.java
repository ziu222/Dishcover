package com.dishcover.rag.service;

import com.dishcover.rag.client.DietaryPreferenceDto;
import com.dishcover.rag.client.RagUserClient;
import com.dishcover.rag.dto.ChatDtos.ChatRequest;
import com.dishcover.rag.dto.ChatDtos.ChatResponse;
import com.dishcover.rag.llm.ConversationTurn;
import com.dishcover.rag.llm.LlmChatResult;
import com.dishcover.rag.llm.LlmGateway;
import com.dishcover.rag.llm.PromptBuilder;
import com.dishcover.rag.pipeline.HybridRetriever;
import com.dishcover.rag.pipeline.IngredientExtractor;
import com.dishcover.rag.pipeline.RetrievedRecipe;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Ghép toàn bộ pipeline giai đoạn A (specs/rag-service.md mục 3.6). */
@Service
public class ChatOrchestrator {

    private final IngredientExtractor ingredientExtractor;
    private final HybridRetriever hybridRetriever;
    private final RagUserClient ragUserClient;
    private final PromptBuilder promptBuilder;
    private final LlmGateway llmGateway;
    private final ConversationHistoryStore historyStore;

    public ChatOrchestrator(IngredientExtractor ingredientExtractor, HybridRetriever hybridRetriever,
                             RagUserClient ragUserClient, PromptBuilder promptBuilder,
                             LlmGateway llmGateway, ConversationHistoryStore historyStore) {
        this.ingredientExtractor = ingredientExtractor;
        this.hybridRetriever = hybridRetriever;
        this.ragUserClient = ragUserClient;
        this.promptBuilder = promptBuilder;
        this.llmGateway = llmGateway;
        this.historyStore = historyStore;
    }

    /**
     * Chạy toàn bộ pipeline giai đoạn A cho 1 lượt chat: trích xuất nguyên liệu từ câu hỏi →
     * lọc cứng qua Matching Service → lấy dietary-preferences thật → dựng prompt kèm lịch sử →
     * gọi LLM (có fallback) → cập nhật lịch sử hội thoại (chỉ khi không fallback) → trả kết quả
     * kèm {@code sourceRecipeIds}.
     *
     * @param bearerToken token JWT của user, chuyển tiếp cho Matching/User Service
     * @param request     câu hỏi + conversationId (nullable)
     * @return câu trả lời kèm nguồn công thức và cờ fallback
     * @throws com.dishcover.rag.exception.ApiExceptions.UpstreamUnavailableException nếu User
     *         Service không xác nhận được dietary-preferences (fail-closed)
     */
    public ChatResponse handle(String bearerToken, ChatRequest request) {
        List<String> ingredients = ingredientExtractor.extract(request.message());
        List<RetrievedRecipe> candidates = hybridRetriever.retrieve(bearerToken, request.message(), ingredients);

        String dietaryText = formatDietaryText(ragUserClient.getDietaryPreferences(bearerToken));
        List<ConversationTurn> history = historyStore.recentTurns(request.conversationId());

        String prompt = promptBuilder.build(dietaryText, candidates, history, request.message());
        LlmChatResult result = llmGateway.chat(prompt);

        String answer;
        boolean fallback;
        if (result.usedFallback()) {
            answer = formatRawFallbackAnswer(candidates);
            fallback = true;
        } else {
            answer = result.answer();
            fallback = false;
        }

        // Không lưu câu trả lời fallback vào lịch sử -- đó là text template tĩnh, không phải nội
        // dung LLM thật, đưa vào {history} của lượt sau chỉ gây nhiễu prompt vô ích.
        if (request.conversationId() != null && !fallback) {
            historyStore.append(request.conversationId(), "user", request.message());
            historyStore.append(request.conversationId(), "assistant", answer);
        }

        List<String> sourceRecipeIds = candidates.stream().map(RetrievedRecipe::recipeId).toList();
        return new ChatResponse(answer, sourceRecipeIds, fallback);
    }

    /** ALLERGY + DIET đều đưa vào text — DIET chỉ ràng buộc mềm ở prompt (mục 1.3), ALLERGY đã lọc cứng ở HybridRetriever rồi. */
    private String formatDietaryText(List<DietaryPreferenceDto> prefs) {
        if (prefs.isEmpty()) {
            return "(không có thông tin)";
        }
        String allergies = prefs.stream()
                .filter(p -> "ALLERGY".equals(p.type()))
                .map(DietaryPreferenceDto::value)
                .collect(Collectors.joining(", "));
        String diets = prefs.stream()
                .filter(p -> "DIET".equals(p.type()))
                .map(DietaryPreferenceDto::value)
                .collect(Collectors.joining(", "));

        List<String> parts = new ArrayList<>();
        if (!allergies.isBlank()) {
            parts.add("Dị ứng: " + allergies);
        }
        if (!diets.isBlank()) {
            parts.add("Chế độ ăn: " + diets);
        }
        return parts.isEmpty() ? "(không có thông tin)" : String.join("; ", parts);
    }

    /** LLM lỗi/timeout -> trả danh sách công thức thô, KHÔNG chết chức năng (mục 3.7). */
    private String formatRawFallbackAnswer(List<RetrievedRecipe> candidates) {
        if (candidates.isEmpty()) {
            return "Trợ lý AI hiện đang tạm thời không phản hồi được, và cũng không tìm thấy công "
                    + "thức phù hợp. Vui lòng thử lại sau.";
        }
        String list = IntStream.range(0, candidates.size())
                .mapToObj(i -> {
                    RetrievedRecipe r = candidates.get(i);
                    return "%d. %s — đã có: %s; cần thêm: %s".formatted(i + 1, r.name(),
                            String.join(", ", r.matchedIngredients()), String.join(", ", r.missingIngredients()));
                })
                .collect(Collectors.joining("\n"));
        return "Trợ lý AI hiện đang tạm thời không phản hồi được. Dưới đây là các công thức phù "
                + "hợp nhất với nguyên liệu bạn nhắc tới:\n" + list;
    }
}
