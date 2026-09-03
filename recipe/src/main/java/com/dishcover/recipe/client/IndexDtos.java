package com.dishcover.recipe.client;

import java.util.Map;

/** Map DTO của Matching Service {@code POST /internal/index} (matching/dto/IndexDtos.java). */
public final class IndexDtos {

    private IndexDtos() {
    }

    public record IndexRequest(String recipeId, String content, float[] embedding, Map<String, Object> metadata) {
    }
}
