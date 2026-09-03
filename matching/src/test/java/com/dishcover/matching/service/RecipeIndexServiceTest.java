package com.dishcover.matching.service;

import com.dishcover.matching.dto.IndexDtos.IndexRequest;
import com.dishcover.matching.dto.IndexDtos.VectorMatch;
import com.dishcover.matching.dto.IndexDtos.VectorSearchRequest;
import com.dishcover.matching.repository.RecipeEmbeddingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeIndexServiceTest {

    private final RecipeEmbeddingRepository repo = Mockito.mock(RecipeEmbeddingRepository.class);
    private final RecipeIndexService service = new RecipeIndexService(repo);

    @Test
    void indexDelegatesToRepositoryUpsert() {
        float[] embedding = {0.1f, 0.2f};
        service.index(new IndexRequest("r1", "noi dung", embedding, Map.of("name", "Pho bo")));

        verify(repo).upsert("r1", "noi dung", embedding, Map.of("name", "Pho bo"));
    }

    @Test
    void searchDelegatesToRepositoryFindNearest() {
        float[] embedding = {0.3f};
        when(repo.findNearest(embedding, 8)).thenReturn(List.of(new VectorMatch("r7", 0.9)));

        List<VectorMatch> result = service.search(new VectorSearchRequest(embedding, 8));

        assertEquals(1, result.size());
        assertEquals("r7", result.get(0).recipeId());
    }
}
