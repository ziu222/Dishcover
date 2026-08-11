package com.dishcover.rag.pipeline;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;
import com.dishcover.common.text.VietnameseTextNormalizer;
import com.dishcover.rag.client.DietaryPreferenceDto;
import com.dishcover.rag.client.RagMatchingClient;
import com.dishcover.rag.client.RagUserClient;
import com.dishcover.rag.client.RecipeMatchDto;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Kênh lọc cứng theo nguyên liệu qua Matching Service (specs/rag-service.md mục 3.2/1.3).
 *
 * <p><b>Lưu ý tên gọi</b>: tên "Hybrid" theo đúng danh sách 5 class CLAUDE.md mục 9 bắt buộc,
 * NHƯNG giai đoạn A chỉ có 1 kênh (qua Matching Service) — chưa có kênh vector, chưa có bước hợp
 * nhất. Giai đoạn B (ngoài phạm vi) sẽ mở rộng THÊM kênh vector + bước merge VÀO CHÍNH class này
 * (không tạo class mới, không đổi tên) — để chữ ký gọi từ {@code ChatOrchestrator} không đổi
 * giữa 2 giai đoạn.</p>
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
    private final RagUserClient ragUserClient;
    private final IngredientCatalog catalog;

    public HybridRetriever(RagMatchingClient ragMatchingClient, RagUserClient ragUserClient,
                            IngredientCatalog catalog) {
        this.ragMatchingClient = ragMatchingClient;
        this.ragUserClient = ragUserClient;
        this.catalog = catalog;
    }

    /**
     * Lấy top công thức khớp nguyên liệu, đã lọc dị ứng thật của user.
     *
     * @param bearerToken         token JWT chuyển tiếp cho Matching/User Service
     * @param extractedIngredients nguyên liệu đã trích xuất từ câu hỏi (IngredientExtractor)
     * @return tối đa {@link #TOP_N} công thức an toàn (không chứa nguyên liệu dị ứng), rỗng nếu
     *         {@code extractedIngredients} rỗng hoặc Matching Service không trả kết quả nào
     */
    public List<RetrievedRecipe> retrieve(String bearerToken, List<String> extractedIngredients) {
        if (extractedIngredients.isEmpty()) {
            return List.of(); // không có gì để chấm điểm -> khỏi gọi Matching, đỡ tốn network
        }
        List<RecipeMatchDto> candidates = ragMatchingClient.searchByIngredients(bearerToken, extractedIngredients, FETCH_N);
        Set<String> allergens = allergenGroupsOf(ragUserClient.getDietaryPreferences(bearerToken));
        return candidates.stream()
                .filter(c -> !violatesAllergy(c, allergens))
                .limit(TOP_N)
                .map(this::toRetrieved)
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
    private boolean violatesAllergy(RecipeMatchDto c, Set<String> allergens) {
        if (allergens.isEmpty()) {
            return false;
        }
        Set<String> allIngredients = new HashSet<>(c.matchedIngredients());
        allIngredients.addAll(c.missingIngredients());
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
}
