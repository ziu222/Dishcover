package com.dishcover.matching.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Map phần cần dùng của JSON Page&lt;T&gt; (Spring Data) trả về từ GET /recipes — bỏ qua field còn
 * lại (totalElements, pageable...).
 *
 * @param <T> kiểu phần tử trong trang dữ liệu
 * @param content danh sách phần tử của trang hiện tại
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageDto<T>(List<T> content) {
}
