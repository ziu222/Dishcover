package com.dishcover.rag.service;

import com.dishcover.rag.client.DietaryPreferenceDto;
import com.dishcover.rag.client.RagUserClient;
import com.dishcover.rag.dto.ChatDtos.ChatRequest;
import com.dishcover.rag.dto.ChatDtos.ChatResponse;
import com.dishcover.rag.llm.LlmChatResult;
import com.dishcover.rag.llm.LlmGateway;
import com.dishcover.rag.llm.PromptBuilder;
import com.dishcover.rag.pipeline.HybridRetriever;
import com.dishcover.rag.pipeline.IngredientExtractor;
import com.dishcover.rag.pipeline.RetrievedRecipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Test wiring toàn bộ pipeline — 4 collaborator mock, verify đúng thứ tự + cả 2 nhánh response. */
class ChatOrchestratorTest {

    private static final String BEARER = "Bearer x";

    private final IngredientExtractor extractor = Mockito.mock(IngredientExtractor.class);
    private final HybridRetriever retriever = Mockito.mock(HybridRetriever.class);
    private final RagUserClient userClient = Mockito.mock(RagUserClient.class);
    private final PromptBuilder promptBuilder = Mockito.mock(PromptBuilder.class);
    private final LlmGateway llmGateway = Mockito.mock(LlmGateway.class);
    private final ConversationHistoryStore historyStore = Mockito.mock(ConversationHistoryStore.class);

    private final ChatOrchestrator orchestrator = new ChatOrchestrator(
            extractor, retriever, userClient, promptBuilder, llmGateway, historyStore);

    private final RetrievedRecipe candidate = new RetrievedRecipe(
            "r1", "Trứng chiên cà chua", "trung-chien-ca-chua",
            List.of("trung ga"), List.of("ca chua"), null);

    @BeforeEach
    void commonStubs() {
        when(extractor.extract(anyString())).thenReturn(List.of("trung ga"));
        when(retriever.retrieve(anyString(), any())).thenReturn(List.of(candidate));
        when(userClient.getDietaryPreferences(anyString())).thenReturn(List.of());
        when(historyStore.recentTurns(any())).thenReturn(List.of());
        when(promptBuilder.build(any(), any(), any(), any())).thenReturn("prompt giả lập");
    }

    @Test
    void llmSuccessReturnsAnswerWithoutFallback() {
        when(llmGateway.chat(anyString())).thenReturn(new LlmChatResult("Nấu trứng chiên nhé", false));

        ChatResponse response = orchestrator.handle(BEARER, new ChatRequest("tôi có trứng gà", null));

        assertFalse(response.fallback());
        assertEquals("Nấu trứng chiên nhé", response.answer());
        assertEquals(List.of("r1"), response.sourceRecipeIds());
    }

    @Test
    void llmFallbackReturnsRawRetrievalListNotBlank() {
        when(llmGateway.chat(anyString())).thenReturn(new LlmChatResult(null, true));

        ChatResponse response = orchestrator.handle(BEARER, new ChatRequest("tôi có trứng gà", null));

        assertTrue(response.fallback());
        assertTrue(response.answer().contains("Trứng chiên cà chua"));
        assertEquals(List.of("r1"), response.sourceRecipeIds());
    }

    @Test
    void nullConversationIdSkipsHistoryReadWrite() {
        when(llmGateway.chat(anyString())).thenReturn(new LlmChatResult("ok", false));

        orchestrator.handle(BEARER, new ChatRequest("tôi có trứng gà", null));

        Mockito.verify(historyStore, Mockito.never()).append(any(), any(), any());
    }

    @Test
    void presentConversationIdAppendsBothTurns() {
        when(llmGateway.chat(anyString())).thenReturn(new LlmChatResult("ok", false));

        orchestrator.handle(BEARER, new ChatRequest("tôi có trứng gà", "conv-1"));

        Mockito.verify(historyStore).append("conv-1", "user", "tôi có trứng gà");
        Mockito.verify(historyStore).append("conv-1", "assistant", "ok");
    }

    @Test
    void dietaryTextIncludesAllergyAndDietSeparately() {
        when(userClient.getDietaryPreferences(anyString())).thenReturn(List.of(
                new DietaryPreferenceDto(1L, "ALLERGY", "hải sản"),
                new DietaryPreferenceDto(2L, "DIET", "chay")));
        when(llmGateway.chat(anyString())).thenReturn(new LlmChatResult("ok", false));

        orchestrator.handle(BEARER, new ChatRequest("tôi có trứng gà", null));

        Mockito.verify(promptBuilder).build(
                org.mockito.ArgumentMatchers.eq("Dị ứng: hải sản; Chế độ ăn: chay"),
                any(), any(), any());
    }
}
