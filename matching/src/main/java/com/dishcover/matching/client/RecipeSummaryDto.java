package com.dishcover.matching.client;

/** Map 1 phần tử content[] của GET /recipes (Page<RecipeSummaryResponse>) — chỉ cần id để gọi tiếp GET /recipes/{id}. */
public record RecipeSummaryDto(String id) {
}
