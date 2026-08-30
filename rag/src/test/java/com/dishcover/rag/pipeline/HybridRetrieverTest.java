package com.dishcover.rag.pipeline;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;
import com.dishcover.rag.client.DietaryPreferenceDto;
import com.dishcover.rag.client.RagMatchingClient;
import com.dishcover.rag.client.RagRecipeClient;
import com.dishcover.rag.client.RagUserClient;
import com.dishcover.rag.client.RecipeDetailDto;
import com.dishcover.rag.client.RecipeIngredientDto;
import com.dishcover.rag.client.RecipeMatchDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class HybridRetrieverTest {

    private final IngredientCatalog catalog = new IngredientCatalog(List.of(
            new IngredientEntry("Trứng gà", "trung ga", List.of(), "dam_dong_vat", 21, "trung"),
            new IngredientEntry("Tôm", "tom", List.of(), "hai_san", 3, "hai_san"),
            new IngredientEntry("Cà chua", "ca chua", List.of(), "rau_cu", 7, null)));

    private final RagMatchingClient matchingClient = Mockito.mock(RagMatchingClient.class);
    private final RagRecipeClient recipeClient = Mockito.mock(RagRecipeClient.class);
    private final RagUserClient userClient = Mockito.mock(RagUserClient.class);
    private final HybridRetriever retriever = new HybridRetriever(matchingClient, recipeClient, userClient, catalog);

    @BeforeEach
    void noNameMatchByDefault() {
        when(recipeClient.searchByName(anyString())).thenReturn(List.of());
    }

    @Test
    void emptyExtractedIngredientsSkipsMatchingButStillTriesNameChannel() {
        List<RetrievedRecipe> result = retriever.retrieve("Bearer x", "Tiramisu nấu ra sao?", List.of());
        assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(matchingClient);
    }

    @Test
    void filtersOutCandidateViolatingUserAllergy() {
        when(matchingClient.searchByIngredients(anyString(), any(), anyInt())).thenReturn(List.of(
                new RecipeMatchDto("r1", "Trứng chiên", "trung-chien", 0.8,
                        List.of("trung ga"), List.of(), null),
                new RecipeMatchDto("r2", "Tôm rang me", "tom-rang-me", 0.9,
                        List.of("tom"), List.of(), null)));
        when(userClient.getDietaryPreferences(anyString())).thenReturn(List.of(
                new DietaryPreferenceDto(1L, "ALLERGY", "hải sản")));

        List<RetrievedRecipe> result = retriever.retrieve("Bearer x", "trứng và tôm", List.of("trung ga", "tom"));

        // r2 chứa "tom" thuộc allergenGroup "hai_san" mà user dị ứng -> bị loại
        assertEquals(1, result.size());
        assertEquals("r1", result.get(0).recipeId());
    }

    @Test
    void noAllergyKeepsAllCandidates() {
        when(matchingClient.searchByIngredients(anyString(), any(), anyInt())).thenReturn(List.of(
                new RecipeMatchDto("r1", "Trứng chiên cà chua", "trung-chien-ca-chua", 0.8,
                        List.of("trung ga", "ca chua"), List.of(), null)));
        when(userClient.getDietaryPreferences(anyString())).thenReturn(List.of());

        List<RetrievedRecipe> result = retriever.retrieve("Bearer x", "trứng và cà chua", List.of("trung ga", "ca chua"));

        assertEquals(1, result.size());
        assertEquals("r1", result.get(0).recipeId());
    }

    @Test
    void nameChannelRecoversExactDishNameQuestionWithNoIngredients() {
        when(recipeClient.searchByName(anyString())).thenReturn(List.of(
                new RecipeDetailDto("r9", "Tiramisu", "tiramisu",
                        List.of(new RecipeIngredientDto("Bánh quy"), new RecipeIngredientDto("Cà phê")))));
        when(userClient.getDietaryPreferences(anyString())).thenReturn(List.of());

        List<RetrievedRecipe> result = retriever.retrieve("Bearer x", "Tiramisu nấu ra sao?", List.of());

        assertEquals(1, result.size());
        assertEquals("r9", result.get(0).recipeId());
        assertEquals(List.of("Bánh quy", "Cà phê"), result.get(0).missingIngredients());
        Mockito.verifyNoInteractions(matchingClient);
    }

    @Test
    void nameChannelCandidateStillFilteredByAllergy() {
        when(recipeClient.searchByName(anyString())).thenReturn(List.of(
                new RecipeDetailDto("r9", "Gỏi tôm", "goi-tom", List.of(new RecipeIngredientDto("Tôm")))));
        when(userClient.getDietaryPreferences(anyString())).thenReturn(List.of(
                new DietaryPreferenceDto(1L, "ALLERGY", "hải sản")));

        List<RetrievedRecipe> result = retriever.retrieve("Bearer x", "Gỏi tôm làm sao?", List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void mergedChannelsDedupeSameRecipeId() {
        when(matchingClient.searchByIngredients(anyString(), any(), anyInt())).thenReturn(List.of(
                new RecipeMatchDto("r1", "Trứng chiên cà chua", "trung-chien-ca-chua", 0.8,
                        List.of("trung ga", "ca chua"), List.of(), null)));
        when(recipeClient.searchByName(anyString())).thenReturn(List.of(
                new RecipeDetailDto("r1", "Trứng chiên cà chua", "trung-chien-ca-chua",
                        List.of(new RecipeIngredientDto("Trứng gà")))));
        when(userClient.getDietaryPreferences(anyString())).thenReturn(List.of());

        List<RetrievedRecipe> result = retriever.retrieve("Bearer x", "trứng chiên cà chua", List.of("trung ga", "ca chua"));

        // trùng recipeId -> chỉ giữ 1 (ưu tiên bản từ kênh nguyên liệu, có matchedIngredients)
        assertEquals(1, result.size());
        assertEquals(List.of("trung ga", "ca chua"), result.get(0).matchedIngredients());
    }
}
