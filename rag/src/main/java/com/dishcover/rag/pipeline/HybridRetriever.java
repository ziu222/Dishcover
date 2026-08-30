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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        Map<String, RetrievedRecipe> merged = new LinkedHashMap<>();
        for (RecipeMatchDto c : byIngredient) {
            merged.put(c.recipeId(), toRetrieved(c));
        }
        for (RecipeDetailDto r : byName) {
            merged.putIfAbsent(r.id(), toRetrieved(r));
        }
        for (RecipeDetailDto r : byCategory) {
            merged.putIfAbsent(r.id(), toRetrieved(r));
        }

        Set<String> allergens = allergenGroupsOf(ragUserClient.getDietaryPreferences(bearerToken));
        return merged.values().stream()
                .filter(r -> !violatesAllergy(r, allergens))
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

    /** Tái tạo lại tập nguyên liệu đầy đủ của công thức (matched ∪ missing = R gốc, mục 1.3), tra allergenGroup. */
    private boolean violatesAllergy(RetrievedRecipe r, Set<String> allergens) {
        if (allergens.isEmpty()) {
            return false;
        }
        Set<String> allIngredients = new HashSet<>(r.matchedIngredients());
        allIngredients.addAll(r.missingIngredients());
        return allIngredients.stream()
                .anyMatch(name -> catalog.lookup(name)
                        .map(IngredientEntry::allergenGroup)
                        .filter(group -> group != null && allergens.contains(group))
                        .isPresent());
    }

    private RetrievedRecipe toRetrieved(RecipeMatchDto c) {
        return new RetrievedRecipe(c.recipeId(), c.name(), c.slug(),
                c.matchedIngredients(), c.missingIngredients(), c.imageUrl());
    }

    /** Khớp qua tên món (không phải nguyên liệu người dùng có) -> toàn bộ nguyên liệu là "cần thêm". */
    private RetrievedRecipe toRetrieved(RecipeDetailDto r) {
        List<String> ingredientNames = r.ingredients() == null
                ? List.of()
                : r.ingredients().stream().map(RecipeIngredientDto::name).toList();
        return new RetrievedRecipe(r.id(), r.name(), r.slug(), List.of(), ingredientNames, null);
    }
}
