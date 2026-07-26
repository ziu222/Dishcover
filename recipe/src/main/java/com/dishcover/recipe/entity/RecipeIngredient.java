package com.dishcover.recipe.entity;

import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Nested document trong Recipe.ingredients — KHÔNG phải collection riêng.
 * essential/weight BẮT BUỘC có (CLAUDE.md mục 3.2) — Matching/RAG dùng trực tiếp để tính điểm.
 */
public class RecipeIngredient {

    @Field("ingredient_id")
    private String ingredientId;

    private String name;

    @Field("normalized_name")
    private String normalizedName;

    private Double amount;
    private String unit;
    private boolean essential;
    private double weight;

    protected RecipeIngredient() {
    }

    public RecipeIngredient(String ingredientId, String name, String normalizedName,
                            Double amount, String unit, boolean essential, double weight) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.amount = amount;
        this.unit = unit;
        this.essential = essential;
        this.weight = weight;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public Double getAmount() {
        return amount;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isEssential() {
        return essential;
    }

    public double getWeight() {
        return weight;
    }
}
