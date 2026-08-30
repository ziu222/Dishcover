package com.dishcover.rag.client;

/** Chỉ cần tên hiển thị — {@link com.dishcover.common.ingredient.IngredientCatalog#lookup} tự normalize/resolve alias. */
public record RecipeIngredientDto(String name) {
}
