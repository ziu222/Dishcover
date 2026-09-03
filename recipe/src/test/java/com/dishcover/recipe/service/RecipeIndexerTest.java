package com.dishcover.recipe.service;

import com.dishcover.recipe.client.MatchingIndexClient;
import com.dishcover.recipe.client.RagIndexClient;
import com.dishcover.recipe.entity.Recipe;
import com.dishcover.recipe.entity.RecipeIngredient;
import com.dishcover.recipe.entity.RecipeStep;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecipeIndexerTest {

    private final RagIndexClient ragIndexClient = Mockito.mock(RagIndexClient.class);
    private final MatchingIndexClient matchingIndexClient = Mockito.mock(MatchingIndexClient.class);
    private final RecipeIndexer indexer = new RecipeIndexer(ragIndexClient, matchingIndexClient);

    private Recipe sampleRecipe() {
        return new Recipe("r1", "Trứng chiên cà chua", "trung-chien-ca-chua", 15, "EASY",
                List.of("nhanh"), List.of("contains_egg"),
                List.of(new RecipeIngredient("ing_trung_ga", "Trứng gà", "trung ga", 2.0, "quả", true, 1.0)),
                List.of(new RecipeStep(1, "Sơ chế", "Đập trứng ra bát", 5)),
                2, null, null, null);
    }

    @Test
    void indexSyncBuildsContentEmbedsThenIndexesInMatching() {
        when(ragIndexClient.embed(anyString(), anyString())).thenReturn(Optional.of(new float[]{0.1f, 0.2f}));

        indexer.indexSync("Bearer x", sampleRecipe());

        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(ragIndexClient).embed(eq("Bearer x"), content.capture());
        assertTrue(content.getValue().contains("Trứng chiên cà chua"));
        assertTrue(content.getValue().contains("Trứng gà"));
        assertTrue(content.getValue().contains("15"));
        assertTrue(content.getValue().contains("nhanh"));
        assertTrue(content.getValue().contains("Đập trứng ra bát"));

        ArgumentCaptor<float[]> embedding = ArgumentCaptor.forClass(float[].class);
        verify(matchingIndexClient).index(eq("Bearer x"), eq("r1"), eq(content.getValue()),
                embedding.capture(), eq(Map.of("name", "Trứng chiên cà chua")));
        assertArrayEquals(new float[]{0.1f, 0.2f}, embedding.getValue());
    }

    @Test
    void indexSyncSkipsMatchingWhenEmbeddingFails() {
        when(ragIndexClient.embed(anyString(), anyString())).thenReturn(Optional.empty());

        indexer.indexSync("Bearer x", sampleRecipe());

        verifyNoInteractions(matchingIndexClient);
    }
}
