package com.dishcover.recipe.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/** Map document collection "recipes" (CLAUDE.md mục 3.2). _id là chuỗi có ý nghĩa (VD "vn_ca_kho_to"), không tự sinh. */
@Document(collection = "recipes")
public class Recipe {

    @Id
    private String id;

    private String name;

    /** name đã chuẩn hóa (bỏ dấu, lowercase) — để tìm kiếm không phân biệt dấu, giống normalized_name của nguyên liệu. */
    @Field("normalized_name")
    private String normalizedName;

    private String slug;

    @Field("cook_time_minutes")
    private int cookTimeMinutes;

    private String difficulty; // EASY | MEDIUM | HARD
    private List<String> tags;

    @Field("dietary_flags")
    private List<String> dietaryFlags;

    private List<RecipeIngredient> ingredients;
    private List<RecipeStep> steps;

    @Field("image_url")
    private String imageUrl;

    @Field("video_url")
    private String videoUrl;

    @Field("created_at")
    private Instant createdAt;

    protected Recipe() {
    }

    public Recipe(String id, String name, String slug, int cookTimeMinutes, String difficulty,
                  List<String> tags, List<String> dietaryFlags, List<RecipeIngredient> ingredients,
                  List<RecipeStep> steps, String imageUrl, String videoUrl) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.cookTimeMinutes = cookTimeMinutes;
        this.difficulty = difficulty;
        this.tags = tags;
        this.dietaryFlags = dietaryFlags;
        this.ingredients = ingredients;
        this.steps = steps;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public String getSlug() {
        return slug;
    }

    public int getCookTimeMinutes() {
        return cookTimeMinutes;
    }

    public void setCookTimeMinutes(int cookTimeMinutes) {
        this.cookTimeMinutes = cookTimeMinutes;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getDietaryFlags() {
        return dietaryFlags;
    }

    public void setDietaryFlags(List<String> dietaryFlags) {
        this.dietaryFlags = dietaryFlags;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    public List<RecipeStep> getSteps() {
        return steps;
    }

    public void setSteps(List<RecipeStep> steps) {
        this.steps = steps;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
