package com.dishcover.recipe.seed;

import com.dishcover.common.security.JwtService;
import com.dishcover.recipe.entity.Recipe;
import com.dishcover.recipe.repository.RecipeRepository;
import com.dishcover.recipe.service.RecipeIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Index toàn bộ công thức ĐANG CÓ vào {@code recipe_embeddings} (Giai đoạn B, chạy 1 lần cho dữ
 * liệu cũ — công thức tạo/sửa SAU thời điểm này tự index qua {@code RecipeIndexer.indexAsync} lúc
 * lưu, không cần runner này nữa). Idempotent tự nhiên: mỗi lần index xóa-rồi-ghi lô cũ (xem
 * {@code RecipeEmbeddingRepository.upsert}), chạy lại an toàn.
 *
 * <p>Gọi {@code indexSync} (đồng bộ, KHÔNG dùng {@code indexAsync}) — chờ xong từng công thức mới
 * qua công thức kế tiếp, để tiến trình chỉ thoát sau khi backfill THẬT SỰ hoàn tất (gọi bản async ở
 * đây sẽ mất hết việc đang chạy nền nếu JVM bị dừng trước khi các thread nền kịp xong).</p>
 *
 * <p>Cần JWT hợp lệ để gọi RAG/Matching (2 endpoint đều yêu cầu) — tự phát hành 1 token hệ thống
 * bằng {@link JwtService} đã có sẵn (cùng secret {@code JWT_SECRET} toàn hệ thống), không phải
 * token của user thật nào (indexing là việc hệ thống, không gắn với 1 người dùng cụ thể).</p>
 */
@Component
@Profile("backfill-embeddings")
public class RecipeIndexBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RecipeIndexBackfillRunner.class);
    private static final int PAGE_SIZE = 50;

    private final RecipeRepository repo;
    private final RecipeIndexer indexer;
    private final JwtService jwtService;

    public RecipeIndexBackfillRunner(RecipeRepository repo, RecipeIndexer indexer, JwtService jwtService) {
        this.repo = repo;
        this.indexer = indexer;
        this.jwtService = jwtService;
    }

    @Override
    public void run(String... args) {
        String token = "Bearer " + jwtService.issue(0L, "system@internal", "FREE");
        int page = 0;
        int total = 0;
        while (true) {
            var batch = repo.findAll(PageRequest.of(page, PAGE_SIZE));
            for (Recipe recipe : batch) {
                indexer.indexSync(token, recipe);
                total++;
            }
            log.info("Đã index {} công thức...", total);
            if (batch.isLast()) {
                break;
            }
            page++;
        }
        log.info("Backfill embedding xong: {} công thức.", total);
    }
}
