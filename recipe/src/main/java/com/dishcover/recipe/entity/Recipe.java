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

    /** Constructor rỗng bắt buộc cho Spring Data MongoDB mapping. */
    protected Recipe() {
    }

    /**
     * Khởi tạo một công thức mới. {@code createdAt} tự động gán bằng thời điểm hiện tại.
     *
     * @param id               id có ý nghĩa (VD "vn_ca_kho_to"), không tự sinh ngẫu nhiên
     * @param name             tên công thức
     * @param slug             slug dùng cho URL thân thiện
     * @param cookTimeMinutes  thời gian nấu ước tính, tính bằng phút
     * @param difficulty       độ khó (EASY | MEDIUM | HARD)
     * @param tags             danh sách tag phân loại
     * @param dietaryFlags     danh sách cờ chế độ ăn (VD contains_egg)
     * @param ingredients      danh sách nguyên liệu của công thức
     * @param steps            danh sách bước nấu của công thức
     * @param imageUrl         URL ảnh minh họa (Cloudinary)
     * @param videoUrl         URL video hướng dẫn, có thể null
     */
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

    /** @return id của công thức */
    public String getId() {
        return id;
    }

    /** @return tên công thức */
    public String getName() {
        return name;
    }

    /** @param name tên công thức mới */
    public void setName(String name) {
        this.name = name;
    }

    /** @return tên công thức đã chuẩn hóa (bỏ dấu, lowercase) */
    public String getNormalizedName() {
        return normalizedName;
    }

    /** @param normalizedName tên công thức đã chuẩn hóa mới */
    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    /** @return slug dùng cho URL thân thiện */
    public String getSlug() {
        return slug;
    }

    /** @return thời gian nấu ước tính, tính bằng phút */
    public int getCookTimeMinutes() {
        return cookTimeMinutes;
    }

    /** @param cookTimeMinutes thời gian nấu ước tính mới, tính bằng phút */
    public void setCookTimeMinutes(int cookTimeMinutes) {
        this.cookTimeMinutes = cookTimeMinutes;
    }

    /** @return độ khó của công thức (EASY | MEDIUM | HARD) */
    public String getDifficulty() {
        return difficulty;
    }

    /** @param difficulty độ khó mới (EASY | MEDIUM | HARD) */
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    /** @return danh sách tag phân loại */
    public List<String> getTags() {
        return tags;
    }

    /** @param tags danh sách tag phân loại mới */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /** @return danh sách cờ chế độ ăn */
    public List<String> getDietaryFlags() {
        return dietaryFlags;
    }

    /** @param dietaryFlags danh sách cờ chế độ ăn mới */
    public void setDietaryFlags(List<String> dietaryFlags) {
        this.dietaryFlags = dietaryFlags;
    }

    /** @return danh sách nguyên liệu của công thức */
    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    /** @param ingredients danh sách nguyên liệu mới */
    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    /** @return danh sách bước nấu của công thức */
    public List<RecipeStep> getSteps() {
        return steps;
    }

    /** @param steps danh sách bước nấu mới */
    public void setSteps(List<RecipeStep> steps) {
        this.steps = steps;
    }

    /** @return URL ảnh minh họa */
    public String getImageUrl() {
        return imageUrl;
    }

    /** @param imageUrl URL ảnh minh họa mới */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /** @return URL video hướng dẫn, có thể null */
    public String getVideoUrl() {
        return videoUrl;
    }

    /** @param videoUrl URL video hướng dẫn mới */
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    /** @return thời điểm tạo công thức */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
