package com.dishcover.rag.client;

import com.dishcover.common.text.VietnameseTextNormalizer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2 kênh truy hồi bù cho {@code IngredientExtractor} (chỉ trích được nguyên liệu) của
 * {@link com.dishcover.rag.pipeline.HybridRetriever} — phát hiện qua bộ eval (xem
 * eval/results/chatbot-report.md): câu hỏi hỏi thẳng TÊN MÓN ("Phở bò") hoặc theo TIÊU CHÍ danh
 * mục ("ăn chay", "dưới 20 phút", "dễ làm") trước đây luôn rỗng dù có đáp án đúng thật.
 *
 * <p>Cách khớp tên món: lấy toàn bộ tên công thức (GET /recipes, public, không cần token — chỉ có
 * ~131 món nên 1 lần gọi rẻ hơn N+1 fetch của Matching Service), rồi kiểm tra tên món (đã
 * normalize) có xuất hiện làm chuỗi con trong câu hỏi (đã normalize) hay không — chiều ngược lại
 * so với {@code findByNormalizedNameContaining} bên Recipe Service (query đó tìm theo 1 từ khóa
 * NGẮN, không hợp để truyền cả câu hỏi tự nhiên dài vào).</p>
 *
 * <p>ponytail: so khớp tên bằng {@code contains()} đơn giản, không giới hạn độ dài tối thiểu —
 * rủi ro tên món 1 từ khớp nhầm, nhưng 131 tên hiện tại đều ≥2 từ nên chưa xảy ra. Kênh danh mục
 * chỉ nhận diện 3 tiêu chí cố định (chay/nhanh/dễ) qua từ khóa cứng, không phải NLP thật — đủ cho
 * 4 câu hỏi eval phát hiện được, thêm tiêu chí mới thì thêm 1 nhánh if.</p>
 */
@Component
public class RagRecipeClient {

    private static final Logger log = LoggerFactory.getLogger(RagRecipeClient.class);
    private static final int MAX_MATCHES = 3;
    private static final int DEFAULT_FAST_MINUTES = 20;
    private static final Pattern MINUTES_PATTERN = Pattern.compile("(\\d+)\\s*phut");

    private final RestClient restClient;

    /**
     * @param builder {@code RestClient.Builder} riêng cho client nội bộ (timeout 3s/5s)
     * @param baseUrl địa chỉ Recipe Service, cấu hình qua {@code services.recipe-url}
     */
    public RagRecipeClient(@Qualifier("internalServiceRestClientBuilder") RestClient.Builder builder,
                            @Value("${services.recipe-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Tìm công thức mà TÊN xuất hiện trong câu hỏi, kèm chi tiết nguyên liệu (để dựng prompt và
     * để {@code HybridRetriever} lọc dị ứng đúng như kênh nguyên liệu).
     *
     * @param question câu hỏi gốc của người dùng
     * @return tối đa {@link #MAX_MATCHES} công thức khớp tên, rỗng nếu không khớp/Recipe Service
     *         lỗi (fail-open — thiếu kênh này vẫn còn kênh nguyên liệu)
     */
    @CircuitBreaker(name = "recipe-service", fallbackMethod = "fallbackEmpty")
    public List<RecipeDetailDto> searchByName(String question) {
        String normalizedQuestion = VietnameseTextNormalizer.normalize(question);
        List<RecipeSummaryDto> all = fetchAllSummaries();
        return all.stream()
                .filter(r -> normalizedQuestion.contains(VietnameseTextNormalizer.normalize(r.name())))
                .limit(MAX_MATCHES)
                .map(r -> fetchDetail(r.id()))
                .toList();
    }

    /**
     * Tìm công thức theo tiêu chí danh mục nhận diện được trong câu hỏi (chay / nấu nhanh dưới N
     * phút / dễ làm cho người mới) — union kết quả của mọi tiêu chí khớp được, dedupe theo id.
     *
     * @param question câu hỏi gốc của người dùng
     * @return tối đa {@link #MAX_MATCHES} công thức thoả ít nhất 1 tiêu chí, rỗng nếu câu hỏi
     *         không khớp tiêu chí nào hoặc Recipe Service lỗi (fail-open)
     */
    @CircuitBreaker(name = "recipe-service", fallbackMethod = "fallbackEmpty")
    public List<RecipeDetailDto> searchByCategory(String question) {
        String normalized = VietnameseTextNormalizer.normalize(question);
        Map<String, RecipeSummaryDto> matched = new LinkedHashMap<>();
        boolean isChay = normalized.contains("chay");

        // "chay" là RÀNG BUỘC ĂN KIÊNG (cứng) -- không trộn chung với "dễ làm"/"nhanh" (sở thích
        // mềm) nữa: bug thật tìm được lúc live-verify (eval/results/bao-cao-tong-hop-danh-gia.md
        // mục 3.3) -- câu "Tôi ăn chay, gợi ý vài món chay dễ làm" trước đây trộn cả nhánh
        // difficulty=EASY (không lọc ăn kiêng) lẫn tag=chay, kéo cả "Cá hồi sốt Teriyaki" (hải
        // sản!) vào chung danh sách với món chay thật -- LLM thấy danh sách tự mâu thuẫn nên từ
        // chối luôn cả loạt thay vì chọn lọc. Khi có "chay", CHỈ dùng tag=chay, bỏ qua 2 nhánh kia.
        if (isChay) {
            fetchSummaries("tag=chay").forEach(r -> matched.putIfAbsent(r.id(), r));
        } else {
            if (normalized.contains("de lam") || normalized.contains("moi tap") || normalized.contains("co ban")) {
                fetchSummaries("difficulty=EASY").forEach(r -> matched.putIfAbsent(r.id(), r));
            }
            Matcher m = MINUTES_PATTERN.matcher(normalized);
            boolean hasExplicitMinutes = m.find();
            if (hasExplicitMinutes || normalized.contains("nhanh")) {
                int maxMinutes = hasExplicitMinutes ? Integer.parseInt(m.group(1)) : DEFAULT_FAST_MINUTES;
                fetchAllSummaries().stream()
                        .filter(r -> r.cookTimeMinutes() > 0 && r.cookTimeMinutes() < maxMinutes)
                        .forEach(r -> matched.putIfAbsent(r.id(), r));
            }
        }

        return matched.values().stream()
                .limit(MAX_MATCHES)
                .map(r -> fetchDetail(r.id()))
                .toList();
    }

    private List<RecipeSummaryDto> fetchAllSummaries() {
        return fetchSummaries("");
    }

    /**
     * Lặp qua từng trang tới khi hết (Recipe Service giới hạn cứng max-page-size=100, xem
     * {@link PageDto}) — gọi 1 lần với {@code size=500} sẽ ÂM THẦM bị cắt còn 100 công thức đầu,
     * bỏ sót phần còn lại mà không có dấu hiệu lỗi nào.
     */
    private List<RecipeSummaryDto> fetchSummaries(String filterQueryString) {
        String base = "/recipes?size=100" + (filterQueryString.isEmpty() ? "" : "&" + filterQueryString);
        List<RecipeSummaryDto> all = new ArrayList<>();
        int pageNumber = 0;
        while (true) {
            PageDto<RecipeSummaryDto> page = restClient.get()
                    .uri(base + "&page=" + pageNumber)
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageDto<RecipeSummaryDto>>() {
                    });
            if (page == null || page.content() == null || page.content().isEmpty()) {
                break;
            }
            all.addAll(page.content());
            if (page.last()) {
                break;
            }
            pageNumber++;
        }
        return all;
    }

    private RecipeDetailDto fetchDetail(String id) {
        return restClient.get()
                .uri("/recipes/{id}", id)
                .retrieve()
                .body(RecipeDetailDto.class);
    }

    /** Recipe Service down -> mất kênh này, các kênh còn lại vẫn chạy bình thường (fail-open). */
    @SuppressWarnings("unused")
    private List<RecipeDetailDto> fallbackEmpty(String question, Throwable ex) {
        log.warn("Recipe Service không phản hồi được (kênh tên món/danh mục), fallback rỗng: {}", ex.getMessage());
        return List.of();
    }
}
