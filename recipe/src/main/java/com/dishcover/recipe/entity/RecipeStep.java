package com.dishcover.recipe.entity;

import org.springframework.data.mongodb.core.mapping.Field;

/** Nested document trong Recipe.steps — KHÔNG phải collection riêng. */
public class RecipeStep {

    private int order;
    private String title;
    private String content;

    @Field("duration_minutes")
    private int durationMinutes;

    /** Constructor rỗng bắt buộc cho Spring Data MongoDB mapping. */
    protected RecipeStep() {
    }

    /**
     * Khởi tạo một bước nấu thuộc công thức.
     *
     * @param order            thứ tự bước, bắt đầu từ 1
     * @param title            tiêu đề ngắn gọn của bước
     * @param content          nội dung chi tiết hướng dẫn
     * @param durationMinutes  thời gian ước tính cho bước, tính bằng phút
     */
    public RecipeStep(int order, String title, String content, int durationMinutes) {
        this.order = order;
        this.title = title;
        this.content = content;
        this.durationMinutes = durationMinutes;
    }

    /** @return thứ tự bước nấu */
    public int getOrder() {
        return order;
    }

    /** @return tiêu đề ngắn gọn của bước nấu */
    public String getTitle() {
        return title;
    }

    /** @return nội dung chi tiết hướng dẫn của bước nấu */
    public String getContent() {
        return content;
    }

    /** @return thời gian ước tính cho bước nấu, tính bằng phút */
    public int getDurationMinutes() {
        return durationMinutes;
    }
}
