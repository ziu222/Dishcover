package com.dishcover.common.ingredient;

/**
 * Trọng số essential/phụ dùng chung — nguồn sự thật duy nhất cho quy ước 1.0/0.3
 * (CLAUDE.md mục 3.2/5). Recipe Service dùng để tính `weight` lưu vào document; Matching Service
 * (`EssentialWeightRule`) dùng lại đúng 2 số này khi tính coverage theo nhóm essential/phụ —
 * trước đây mỗi bên tự khai báo hằng số riêng, dễ lệch nếu quy ước đổi (phát hiện qua scan kiến
 * trúc java-architect).
 */
public final class IngredientWeights {

    public static final double ESSENTIAL = 1.0;
    public static final double OPTIONAL = 0.3;

    private IngredientWeights() {
    }
}
