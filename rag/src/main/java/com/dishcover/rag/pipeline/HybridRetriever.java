package com.dishcover.rag.pipeline;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;
import com.dishcover.common.text.VietnameseTextNormalizer;
import com.dishcover.rag.client.DietaryPreferenceDto;
import com.dishcover.rag.client.RagMatchingClient;
import com.dishcover.rag.client.RagRecipeClient;
import com.dishcover.rag.client.RagUserClient;
import com.dishcover.rag.client.RecipeDetailDto;
import com.dishcover.rag.client.RecipeIngredientDto;
import com.dishcover.rag.client.RecipeMatchDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 3 kênh truy hồi giai đoạn A: (1) lọc cứng theo nguyên liệu qua Matching Service (specs/
 * rag-service.md mục 3.2/1.3), (2) tìm theo TÊN MÓN, (3) tìm theo TIÊU CHÍ danh mục (chay/nhanh/
 * dễ) — (2)+(3) qua {@link RagRecipeClient}, thêm sau khi bộ eval phát hiện câu hỏi kiểu "Cho tôi
 * công thức Phở bò" hoặc "món nào dễ làm" luôn bị từ chối dù có đáp án đúng thật, vì kênh (1) chỉ
 * trích được nguyên liệu (xem eval/results/chatbot-report.md).
 *
 * <p><b>Lưu ý tên gọi</b>: tên "Hybrid" theo đúng danh sách 5 class CLAUDE.md mục 9 bắt buộc.
 * Giai đoạn B (ngoài phạm vi, vector search pgvector) sẽ mở rộng THÊM 1 kênh nữa VÀO CHÍNH class
 * này (không tạo class mới, không đổi tên) — để chữ ký gọi từ {@code ChatOrchestrator} không đổi
 * giữa các giai đoạn.</p>
 */
@Component
public class HybridRetriever {

    private static final int TOP_N = 5;
    // Xin dư hơn TOP_N rồi lọc dị ứng MỚI trim -- nếu chỉ xin đúng TOP_N rồi lọc, user dị ứng
    // nguyên liệu phổ biến có thể chỉ còn 0-2 kết quả dù còn nhiều công thức an toàn xếp hạng 6+.
    // 20 = MAX_TOP_N Matching Service tự clamp (matching/service/MatchingService.java), không xin
    // được nhiều hơn nên không cần Math.min thêm ở đây.
    private static final int FETCH_N = 20;

    private final RagMatchingClient ragMatchingClient;
    private final RagRecipeClient ragRecipeClient;
    private final RagUserClient ragUserClient;
    private final IngredientCatalog catalog;

    public HybridRetriever(RagMatchingClient ragMatchingClient, RagRecipeClient ragRecipeClient,
                            RagUserClient ragUserClient, IngredientCatalog catalog) {
        this.ragMatchingClient = ragMatchingClient;
        this.ragRecipeClient = ragRecipeClient;
        this.ragUserClient = ragUserClient;
        this.catalog = catalog;
    }

    /**
     * Lấy top công thức khớp nguyên liệu VÀ/HOẶC khớp tên món, đã lọc dị ứng thật của user.
     *
     * @param bearerToken           token JWT chuyển tiếp cho Matching/User Service
     * @param question              câu hỏi gốc, dùng cho kênh tìm theo tên món
     * @param extractedIngredients  nguyên liệu đã trích xuất từ câu hỏi (IngredientExtractor)
     * @return tối đa {@link #TOP_N} công thức an toàn (không chứa nguyên liệu dị ứng), ưu tiên
     *         khớp nguyên liệu trước, rồi đến khớp tên món (không trùng lặp)
     */
    public List<RetrievedRecipe> retrieve(String bearerToken, String question, List<String> extractedIngredients) {
        List<RecipeMatchDto> byIngredient = extractedIngredients.isEmpty()
                ? List.of() // không có nguyên liệu -> khỏi gọi Matching, đỡ tốn network
                : ragMatchingClient.searchByIngredients(bearerToken, extractedIngredients, FETCH_N);
        List<RecipeDetailDto> byName = ragRecipeClient.searchByName(question);
        List<RecipeDetailDto> byCategory = ragRecipeClient.searchByCategory(question);
        Set<String> allergens = allergenGroupsOf(ragUserClient.getDietaryPreferences(bearerToken));
        Set<String> nameMatchIds = byName.stream().map(RecipeDetailDto::id).collect(Collectors.toSet());

        // Thứ tự chèn = thứ tự ưu tiên khi .limit(TOP_N) cắt bớt: tên món/danh mục là tín hiệu RÕ
        // RÀNG (người dùng hỏi thẳng), phải ưu tiên hơn kênh nguyên liệu (Jaccard, dễ có nhiều kết
        // quả "na ná" lấp đầy hết chỗ) — xem eval/results/bao-cao-tong-hop-danh-gia.md, bug tìm
        // được lúc live-verify: hỏi thẳng "Phở bò" vẫn bị từ chối vì kênh nguyên liệu (không liên
        // quan) đã chiếm đủ 5 chỗ trước khi tới lượt kênh tên món dù nó khớp tuyệt đối.
        Map<String, RetrievedRecipe> merged = new LinkedHashMap<>();
        for (RecipeDetailDto r : byName) {
            merged.putIfAbsent(r.id(), toRetrieved(r, allergens));
        }
        for (RecipeDetailDto r : byCategory) {
            merged.putIfAbsent(r.id(), toRetrieved(r, allergens));
        }
        for (RecipeMatchDto c : byIngredient) {
            // put() (không phải putIfAbsent): nếu trùng id với kênh tên món/danh mục, ưu tiên GIỮ
            // vị trí đã chèn (đầu danh sách) nhưng THAY nội dung bằng bản kênh nguyên liệu — có
            // matchedIngredients/missingIngredients thật để hiển thị "đã có/cần thêm" đúng.
            merged.put(c.recipeId(), toRetrieved(c, allergens));
        }

        // Kênh TÊN MÓN (hỏi thẳng) -> giữ lại dù có dietaryConflicts, chỉ gắn cờ cảnh báo (LLM +
        // frontend xử lý, xem PromptBuilder/ChatResponse.dietaryWarnings) vì người dùng tự quyết
        // định khi đã hỏi đích danh. Kênh khác (gợi ý chủ động) vẫn chặn cứng như trước — hệ thống
        // không tự đề xuất món vi phạm ăn kiêng khi người dùng không hỏi thẳng tên.
        return merged.values().stream()
                .filter(r -> nameMatchIds.contains(r.recipeId()) || r.dietaryConflicts().isEmpty())
                .limit(TOP_N)
                .toList();
    }

    /** Lọc type=ALLERGY, quy đổi value tự do sang slug khớp allergenGroup catalog — đúng transform matching/client/UserClient.java. */
    private Set<String> allergenGroupsOf(List<DietaryPreferenceDto> prefs) {
        return prefs.stream()
                .filter(p -> "ALLERGY".equals(p.type()))
                .map(p -> VietnameseTextNormalizer.normalize(p.value()).replace(' ', '_'))
                .collect(Collectors.toSet());
    }

    /**
     * Tái tạo lại tập nguyên liệu đầy đủ của công thức (matched ∪ missing = R gốc, mục 1.3), tra
     * allergenGroup từng nguyên liệu -- tính SẴN bằng code, không để LLM tự suy đoán (log thật
     * cho thấy LLM suy đoán không nhất quán giữa các câu hỏi tương tự, xem eval/results/
     * bao-cao-tong-hop-danh-gia.md).
     *
     * @return tên hiển thị (canonicalName) các nguyên liệu vi phạm, rỗng nếu không xung đột
     */
    private List<String> computeDietaryConflicts(List<String> matched, List<String> missing, Set<String> allergens) {
        if (allergens.isEmpty()) {
            return List.of();
        }
        Set<String> allIngredients = new LinkedHashSet<>(matched);
        allIngredients.addAll(missing);
        return allIngredients.stream()
                .map(catalog::lookup)
                .flatMap(Optional::stream)
                .filter(e -> e.allergenGroup() != null && allergens.contains(e.allergenGroup()))
                .map(IngredientEntry::canonicalName)
                .distinct()
                .toList();
    }

    private RetrievedRecipe toRetrieved(RecipeMatchDto c, Set<String> allergens) {
        return new RetrievedRecipe(c.recipeId(), c.name(), c.slug(),
                c.matchedIngredients(), c.missingIngredients(), c.imageUrl(),
                computeDietaryConflicts(c.matchedIngredients(), c.missingIngredients(), allergens));
    }

    /** Khớp qua tên món (không phải nguyên liệu người dùng có) -> toàn bộ nguyên liệu là "cần thêm". */
    private RetrievedRecipe toRetrieved(RecipeDetailDto r, Set<String> allergens) {
        List<String> ingredientNames = r.ingredients() == null
                ? List.of()
                : r.ingredients().stream().map(RecipeIngredientDto::name).toList();
        return new RetrievedRecipe(r.id(), r.name(), r.slug(), List.of(), ingredientNames, null,
                computeDietaryConflicts(List.of(), ingredientNames, allergens));
    }
}
