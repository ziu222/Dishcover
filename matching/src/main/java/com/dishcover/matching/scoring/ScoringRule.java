package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;

/** Strategy chấm điểm 1 công thức — CLAUDE.md mục 5, chuỗi Open/Closed (thêm rule mới không sửa rule cũ). */
public interface ScoringRule {

    /**
     * Áp dụng 1 bước chấm điểm/lọc lên công thức, dựa trên điểm đã tính từ (các) rule chạy trước
     * trong chuỗi ({@link com.dishcover.matching.scoring.MatchingEngine}).
     *
     * @param recipe công thức đang được chấm điểm
     * @param ctx dữ liệu nguyên liệu/dị ứng của người dùng, dùng chung cho mọi rule
     * @param currentScore điểm số tích lũy từ (các) rule chạy trước trong chuỗi
     * @return điểm số sau khi áp dụng rule này; {@link Double#NEGATIVE_INFINITY} nếu công thức bị
     *         loại cứng (VD vi phạm dị ứng)
     */
    double apply(RecipeDetailDto recipe, MatchingContext ctx, double currentScore);
}
