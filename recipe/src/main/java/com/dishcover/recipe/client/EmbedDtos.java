package com.dishcover.recipe.client;

/** Map DTO của RAG Service {@code POST /internal/embed} (rag/dto/EmbedDtos.java). */
public final class EmbedDtos {

    private EmbedDtos() {
    }

    public record EmbedRequest(String text) {
    }

    public record EmbedResponse(float[] embedding) {
    }
}
