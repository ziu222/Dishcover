package com.dishcover.matching.service;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.matching.client.InventoryClient;
import com.dishcover.matching.client.InventoryItemDto;
import com.dishcover.matching.client.RecipeClient;
import com.dishcover.matching.client.RecipeDetailDto;
import com.dishcover.matching.client.RecipeIngredientDto;
import com.dishcover.matching.client.UserClient;
import com.dishcover.matching.dto.MatchingDtos.RecipeMatchResponse;
import com.dishcover.matching.scoring.MatchingContext;
import com.dishcover.matching.scoring.MatchingEngine;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Orchestration: gọi 3 service ngoài, chấm điểm, sort, trả top N (specs/matching-service.md mục 3.3). */
@Service
public class MatchingService {

    private static final int DEFAULT_TOP_N = 5;
    private static final int MAX_TOP_N = 20;

    private final InventoryClient inventoryClient;
    private final RecipeClient recipeClient;
    private final UserClient userClient;
    private final MatchingEngine engine;
    private final IngredientCatalog catalog;

    /**
     * @param inventoryClient client gọi Inventory Service lấy nguyên liệu người dùng đang có
     * @param recipeClient client gọi Recipe Service lấy toàn bộ công thức kèm nguyên liệu
     * @param userClient client gọi User Service lấy nhóm dị ứng người dùng
     * @param engine engine chạy chuỗi {@link com.dishcover.matching.scoring.ScoringRule}
     * @param catalog từ điển nguyên liệu chuẩn hóa, dùng để resolve tên nguyên liệu tự do
     */
    public MatchingService(InventoryClient inventoryClient, RecipeClient recipeClient,
                            UserClient userClient, MatchingEngine engine, IngredientCatalog catalog) {
        this.inventoryClient = inventoryClient;
        this.recipeClient = recipeClient;
        this.userClient = userClient;
        this.engine = engine;
        this.catalog = catalog;
    }

    /**
     * Gợi ý công thức theo tủ lạnh + hồ sơ dị ứng thật của người dùng đang đăng nhập: gọi Inventory/
     * User/Recipe Service, chấm điểm qua {@link MatchingEngine}, loại công thức bị lọc cứng
     * ({@link Double#NEGATIVE_INFINITY}), sort giảm dần theo điểm rồi lấy top N.
     *
     * @param bearerToken header Authorization ("Bearer &lt;token&gt;") của người dùng đang gọi,
     *                    dùng để gọi tiếp Inventory/User Service
     * @param topN số lượng kết quả tối đa mong muốn; null dùng mặc định, luôn bị clamp trong
     *             khoảng [1, {@value #MAX_TOP_N}]
     * @return danh sách công thức phù hợp, sắp xếp giảm dần theo điểm số
     */
    public List<RecipeMatchResponse> suggest(String bearerToken, Integer topN) {
        int limit = clamp(topN);

        List<InventoryItemDto> inventory = inventoryClient.getFreshItems(bearerToken);
        Set<String> allergens = userClient.getAllergenGroups(bearerToken);
        List<RecipeDetailDto> recipes = recipeClient.getAllRecipesWithIngredients();

        MatchingContext ctx = buildContext(inventory, allergens);

        return recipes.stream()
                .map(r -> Map.entry(r, engine.score(r, ctx)))
                .filter(e -> e.getValue() > Double.NEGATIVE_INFINITY)
                .sorted(Map.Entry.<RecipeDetailDto, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> toResponse(e.getKey(), e.getValue(), ctx))
                .toList();
    }

    /**
     * Chấm điểm theo 1 danh sách nguyên liệu tùy ý (không gắn với Inventory/User thật của ai) —
     * dùng cho RAG Service trích xuất nguyên liệu từ câu hỏi chat (specs/rag-service.md mục 1.1).
     * expiry rỗng + allergen rỗng khiến ExpiryBonusRule/AllergyFilterRule tự nhiên thành no-op,
     * KHÔNG đổi 1 dòng nào trong ScoringRule/MatchingEngine.
     *
     * @param rawIngredientNames tên nguyên liệu tự do (chưa chuẩn hóa), được resolve qua
     *                           {@link IngredientCatalog} trước khi đưa vào chấm điểm
     * @param topN số lượng kết quả tối đa mong muốn; null dùng mặc định, luôn bị clamp trong
     *             khoảng [1, {@value #MAX_TOP_N}]
     * @return danh sách công thức phù hợp, sắp xếp giảm dần theo điểm số
     */
    public List<RecipeMatchResponse> searchByIngredients(List<String> rawIngredientNames, Integer topN) {
        int limit = clamp(topN);
        Set<String> normalized = rawIngredientNames.stream()
                .map(catalog::resolve)
                .collect(Collectors.toSet());
        MatchingContext ctx = new MatchingContext(normalized, Map.of(), Set.of());
        List<RecipeDetailDto> recipes = recipeClient.getAllRecipesWithIngredients();

        return recipes.stream()
                .map(r -> Map.entry(r, engine.score(r, ctx)))
                .filter(e -> e.getValue() > Double.NEGATIVE_INFINITY)
                .sorted(Map.Entry.<RecipeDetailDto, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> toResponse(e.getKey(), e.getValue(), ctx))
                .toList();
    }

    private MatchingContext buildContext(List<InventoryItemDto> inventory, Set<String> allergens) {
        Set<String> names = inventory.stream()
                .map(InventoryItemDto::normalizedName)
                .collect(Collectors.toSet());
        Map<String, LocalDate> expiry = inventory.stream()
                .filter(i -> i.expiryDate() != null)
                .collect(Collectors.toMap(InventoryItemDto::normalizedName, InventoryItemDto::expiryDate,
                        (first, second) -> first));
        return new MatchingContext(names, expiry, allergens);
    }

    private RecipeMatchResponse toResponse(RecipeDetailDto recipe, double score, MatchingContext ctx) {
        // So khớp bằng normalizedName (khoá không dấu), nhưng trả về client bằng name (tên hiển thị
        // có dấu) — trước đây trả nhầm normalizedName khiến FE hiện "Ca hoi" thay vì "Cá hồi".
        List<String> matched = recipe.ingredients().stream()
                .filter(i -> ctx.userNormalizedNames().contains(i.normalizedName()))
                .map(RecipeIngredientDto::name)
                .toList();
        List<String> missing = recipe.ingredients().stream()
                .filter(i -> !ctx.userNormalizedNames().contains(i.normalizedName()))
                .map(RecipeIngredientDto::name)
                .toList();
        return new RecipeMatchResponse(recipe.id(), recipe.name(), recipe.slug(), score,
                matched, missing, recipe.imageUrl());
    }

    private int clamp(Integer topN) {
        if (topN == null) {
            return DEFAULT_TOP_N;
        }
        return Math.max(1, Math.min(topN, MAX_TOP_N));
    }
}
