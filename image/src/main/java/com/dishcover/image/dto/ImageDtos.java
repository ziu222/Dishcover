package com.dishcover.image.dto;

import java.time.LocalDate;
import java.util.List;

/** DTO cho Image Recognition Service — record, không expose entity (repo không có entity/DB). */
public final class ImageDtos {

    private ImageDtos() {
    }

    /**
     * 1 nguyên liệu Vision API nhận diện được, đã chuẩn hóa qua IngredientCatalog.
     *
     * @param name              tên hiển thị (từ Vision API hoặc canonical name nếu khớp catalog)
     * @param normalizedName    khóa so khớp chuẩn (khớp inventory_service.user_ingredients)
     * @param confidence        độ tin cậy 0..1 do Vision API trả
     * @param quantityGuess     ước lượng số lượng (VD "2 quả"), null nếu không rõ
     * @param suggestedExpiryDate hạn dùng gợi ý — sửa được, KHÔNG tự ghi DB (human-in-the-loop)
     */
    public record RecognizedIngredientDto(
            String name,
            String normalizedName,
            double confidence,
            String quantityGuess,
            LocalDate suggestedExpiryDate
    ) {
    }

    /** Phản hồi nhận diện — client hiển thị màn xác nhận, KHÔNG ghi DB tự động. */
    public record RecognizeResponse(
            List<RecognizedIngredientDto> items
    ) {
    }
}
