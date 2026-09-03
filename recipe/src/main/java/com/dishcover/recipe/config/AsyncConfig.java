package com.dishcover.recipe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Bật {@code @Async} cho {@code RecipeIndexer} — index công thức (Giai đoạn B, gọi RAG+Matching)
 * chạy nền, KHÔNG chặn response của {@code POST}/{@code PATCH /recipes} (best-effort thật sự,
 * không chỉ "không ném lỗi" — nếu chạy đồng bộ, embedding provider chậm/timeout 15-20s sẽ kéo dài
 * response tạo công thức tương ứng, dù cuối cùng vẫn "không lỗi").
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
