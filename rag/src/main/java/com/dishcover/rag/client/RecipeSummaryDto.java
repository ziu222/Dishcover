package com.dishcover.rag.client;

/** Map 1 phần tử content[] của GET /recipes (Page&lt;RecipeSummaryResponse&gt;) — id+name đủ để so khớp tên món. */
public record RecipeSummaryDto(String id, String name) {
}
