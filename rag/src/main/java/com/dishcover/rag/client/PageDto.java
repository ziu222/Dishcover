package com.dishcover.rag.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Map phần cần dùng của JSON Page&lt;T&gt; (Spring Data) trả về từ GET /recipes — bỏ qua field
 * còn lại. {@code last} cần thiết để phân trang đúng: Recipe Service giới hạn cứng
 * {@code max-page-size=100} (application.yml, có chủ đích), nên {@code ?size=500} bị cắt còn
 * đúng 100/131 công thức nếu chỉ gọi 1 lần — phải lặp qua từng trang tới khi {@code last=true}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageDto<T>(List<T> content, boolean last) {
}
