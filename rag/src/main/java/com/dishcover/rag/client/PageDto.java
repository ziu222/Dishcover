package com.dishcover.rag.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Map phần cần dùng của JSON Page&lt;T&gt; (Spring Data) trả về từ GET /recipes — bỏ qua field còn lại. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageDto<T>(List<T> content) {
}
