package com.dishcover.matching.service;

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

    public MatchingService(InventoryClient inventoryClient, RecipeClient recipeClient,
                            UserClient userClient, MatchingEngine engine) {
        this.inventoryClient = inventoryClient;
        this.recipeClient = recipeClient;
        this.userClient = userClient;
        this.engine = engine;
    }

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
        List<String> matched = recipe.ingredients().stream()
                .map(RecipeIngredientDto::normalizedName)
                .filter(ctx.userNormalizedNames()::contains)
                .toList();
        List<String> missing = recipe.ingredients().stream()
                .map(RecipeIngredientDto::normalizedName)
                .filter(n -> !ctx.userNormalizedNames().contains(n))
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
