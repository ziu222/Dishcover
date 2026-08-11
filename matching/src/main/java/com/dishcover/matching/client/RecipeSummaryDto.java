package com.dishcover.matching.client;

/**
 * Map 1 phần tử content[] của GET /recipes (Page&lt;RecipeSummaryResponse&gt;) — chỉ cần id để gọi
 * tiếp GET /recipes/{id}.
 *
 * @param id id công thức, dùng để gọi tiếp GET /recipes/{id} lấy chi tiết ingredients
 */
public record RecipeSummaryDto(String id) {
}
