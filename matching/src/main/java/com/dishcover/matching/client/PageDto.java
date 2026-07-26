package com.dishcover.matching.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Map phần cần dùng của JSON Page<T> (Spring Data) trả về từ GET /recipes — bỏ qua field còn lại (totalElements, pageable...). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageDto<T>(List<T> content) {
}
