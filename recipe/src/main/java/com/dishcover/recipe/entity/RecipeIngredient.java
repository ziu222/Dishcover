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

    /** Constructor rỗng bắt buộc cho Spring Data MongoDB mapping. */
    protected RecipeIngredient() {
    }

    /**
     * Khởi tạo một nguyên liệu thuộc công thức.
     *
     * @param ingredientId    mã định danh nguyên liệu, tự sinh từ tên chuẩn hóa (xem {@code RecipeService})
     * @param name            tên nguyên liệu hiển thị
     * @param normalizedName  tên đã chuẩn hóa (bỏ dấu, lowercase) dùng để so khớp với Inventory/Matching
     * @param amount          số lượng
     * @param unit            đơn vị tính
     * @param essential       true nếu là nguyên liệu chính, false nếu là nguyên liệu phụ
     * @param weight          trọng số dùng cho thuật toán chấm điểm (essential=1.0, phụ=0.3)
     */
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

    /** @return mã định danh nguyên liệu */
    public String getIngredientId() {
        return ingredientId;
    }

    /** @return tên nguyên liệu hiển thị */
    public String getName() {
        return name;
    }

    /** @return tên nguyên liệu đã chuẩn hóa (bỏ dấu, lowercase) */
    public String getNormalizedName() {
        return normalizedName;
    }

    /** @return số lượng nguyên liệu */
    public Double getAmount() {
        return amount;
    }

    /** @return đơn vị tính của nguyên liệu */
    public String getUnit() {
        return unit;
    }

    /** @return true nếu là nguyên liệu chính (essential), false nếu là nguyên liệu phụ */
    public boolean isEssential() {
        return essential;
    }

    /** @return trọng số dùng cho thuật toán chấm điểm matching */
    public double getWeight() {
        return weight;
    }
}
