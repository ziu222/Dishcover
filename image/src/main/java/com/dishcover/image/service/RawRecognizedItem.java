package com.dishcover.image.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 1 nguyên liệu THÔ do Vision API trả về (khớp JSON của VISION_PROMPT, CLAUDE.md mục 7) — CHƯA
 * chuẩn hóa qua Ingredient Catalog. ResultNormalizer sẽ biến nó thành RecognizedIngredientDto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawRecognizedItem(
        String name,
        double confidence,
        @JsonProperty("quantity_guess") String quantityGuess
) {
}
