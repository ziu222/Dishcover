package com.dishcover.rag.client;

import java.util.List;

/** Map DTO của Matching Service {@code POST /internal/vector-search} (matching/dto/IndexDtos.java). */
public final class VectorSearchDtos {

    private VectorSearchDtos() {
    }

    public record VectorSearchRequest(float[] embedding, Integer topK) {
    }

    public record VectorMatch(String recipeId, double similarity) {
    }

    public record VectorSearchResponse(List<VectorMatch> matches) {
    }
}
