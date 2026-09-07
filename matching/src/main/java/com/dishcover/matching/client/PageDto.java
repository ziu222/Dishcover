package com.dishcover.matching.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Map phần cần dùng của JSON Page&lt;T&gt; (Spring Data) trả về từ GET /recipes — bỏ qua field còn
 * lại (totalElements, pageable...).
 *
 * @param <T> kiểu phần tử trong trang dữ liệu
 * @param content danh sách phần tử của trang hiện tại
 * @param last true nếu đây là trang cuối cùng — dùng để biết khi nào dừng lặp fetch thêm trang
 *             (RecipeClient trước đây chỉ lấy đúng 1 trang, bỏ sót công thức khi tổng số vượt
 *             max-page-size của Recipe Service — bug thật phát hiện lúc live-verify Stage 8,
 *             docs/specs/diet-direction-recommendation.md)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageDto<T>(List<T> content, boolean last) {
}
