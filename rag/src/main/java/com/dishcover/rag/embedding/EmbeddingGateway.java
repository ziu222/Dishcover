package com.dishcover.rag.embedding;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Bọc lời gọi embedding bằng Circuit Breaker + TimeLimiter (song song {@code LlmGateway}). Dùng ở
 * 2 nơi: (1) {@code EmbedController} — embed văn bản đại diện công thức, gọi từ Recipe Service lúc
 * index; (2) {@code HybridRetriever} — embed câu hỏi người dùng cho kênh vector search.
 *
 * <p>Trả {@link Optional#empty()} khi lỗi/timeout thay vì ném exception — CẢ 2 nơi gọi đều fail-open
 * (thiếu embedding thì bỏ qua bước đó, không chặn luồng chính: index thất bại không chặn lưu công
 * thức, kênh vector search rỗng thì các kênh Giai đoạn A khác vẫn chạy — không có failure mode mới).</p>
 */
@Component
public class EmbeddingGateway {

    private static final long CALL_TIMEOUT_SECONDS = 20;

    private final ResilientEmbeddingCaller caller;

    public EmbeddingGateway(ResilientEmbeddingCaller caller) {
        this.caller = caller;
    }

    /** embed() blocking — phần còn lại của service (servlet stack) không phải xử lý Future. */
    public Optional<float[]> embed(String text) {
        try {
            return Optional.ofNullable(caller.embedAsync(text).get(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
