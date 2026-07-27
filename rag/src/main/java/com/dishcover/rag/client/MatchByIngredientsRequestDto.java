package com.dishcover.rag.client;

import java.util.List;

/** Body gửi sang POST /matching/internal/match-by-ingredients. */
public record MatchByIngredientsRequestDto(List<String> ingredients, Integer topN) {
}
