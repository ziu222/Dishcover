package com.dishcover.rag.client;

/**
 * Map 1 phần tử content[] của GET /recipes (Page&lt;RecipeSummaryResponse&gt;) — id+name để so
 * khớp tên món, cookTimeMinutes để lọc "nấu nhanh" cục bộ (Recipe Service không có filter thời
 * gian sẵn, không đáng thêm 1 query param mới chỉ cho 1 nhu cầu).
 */
public record RecipeSummaryDto(String id, String name, int cookTimeMinutes) {
}
