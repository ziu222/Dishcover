package com.dishcover.matching.scoring;

import com.dishcover.matching.client.RecipeDetailDto;

import java.util.List;

/** Chạy tuần tự chuỗi ScoringRule cho 1 công thức — thêm rule mới = thêm 1 class + đăng ký ở ScoringConfig. */
public class MatchingEngine {

    private final List<ScoringRule> rules;

    /**
     * @param rules chuỗi rule sẽ chạy tuần tự theo đúng thứ tự trong danh sách (thứ tự do
     *              {@code ScoringConfig} khai báo tường minh, ảnh hưởng trực tiếp kết quả)
     */
    public MatchingEngine(List<ScoringRule> rules) {
        this.rules = rules;
    }

    /**
     * Chạy tuần tự toàn bộ chuỗi {@link ScoringRule} lên 1 công thức, dừng sớm nếu điểm số về
     * {@link Double#NEGATIVE_INFINITY} (bị loại cứng bởi 1 rule, VD dị ứng).
     *
     * @param recipe công thức cần chấm điểm
     * @param ctx dữ liệu nguyên liệu/dị ứng của người dùng, dùng chung cho mọi rule trong chuỗi
     * @return điểm số cuối cùng sau khi chạy hết chuỗi rule (hoặc dừng sớm);
     *         {@link Double#NEGATIVE_INFINITY} nếu công thức bị loại cứng
     */
    public double score(RecipeDetailDto recipe, MatchingContext ctx) {
        double score = 0.0;
        for (ScoringRule rule : rules) {
            score = rule.apply(recipe, ctx, score);
            if (score == Double.NEGATIVE_INFINITY) {
                break;
            }
        }
        return score;
    }
}
