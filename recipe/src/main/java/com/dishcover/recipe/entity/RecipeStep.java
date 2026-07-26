package com.dishcover.recipe.entity;

import org.springframework.data.mongodb.core.mapping.Field;

/** Nested document trong Recipe.steps — KHÔNG phải collection riêng. */
public class RecipeStep {

    private int order;
    private String title;
    private String content;

    @Field("duration_minutes")
    private int durationMinutes;

    protected RecipeStep() {
    }

    public RecipeStep(int order, String title, String content, int durationMinutes) {
        this.order = order;
        this.title = title;
        this.content = content;
        this.durationMinutes = durationMinutes;
    }

    public int getOrder() {
        return order;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }
}
