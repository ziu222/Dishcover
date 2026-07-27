package com.dishcover.rag.pipeline;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.text.VietnameseTextNormalizer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Trích xuất nguyên liệu từ câu hỏi chat bằng dictionary match, greedy longest n-gram
 * (specs/rag-service.md mục 3.1 — thuật toán + ví dụ chạy tay đầy đủ ở đó).
 *
 * <p>Dùng {@link IngredientCatalog#lookup} chứ KHÔNG dùng {@code resolve()} — {@code resolve()}
 * luôn trả về 1 chuỗi (kể cả không có trong catalog, trả lại chính chuỗi đã normalize), không thể
 * dùng để phân biệt "có khớp catalog thật hay không". {@code lookup()} trả {@code Optional}, đúng
 * thứ cần để quyết định 1 cụm từ có phải nguyên liệu thật hay không.</p>
 */
@Component
public class IngredientExtractor {

    // package-private (không private): guard test IngredientExtractorTest so trực tiếp với catalog
    // thật để phát hiện sớm nếu sau này có alias dài hơn MAX_WINDOW từ (âm thầm không khớp được).
    static final int MAX_WINDOW = 4; // alias dài nhất trong catalog chỉ vài từ

    private final IngredientCatalog catalog;

    public IngredientExtractor(IngredientCatalog catalog) {
        this.catalog = catalog;
    }

    public List<String> extract(String question) {
        String normalized = VietnameseTextNormalizer.normalize(question);
        if (normalized.isEmpty()) {
            return List.of();
        }
        String[] tokens = normalized.split(" ");
        int n = tokens.length;
        Set<String> result = new LinkedHashSet<>(); // giữ thứ tự xuất hiện, tự loại trùng

        int i = 0;
        while (i < n) {
            int matchedWindow = 0;
            for (int windowSize = Math.min(MAX_WINDOW, n - i); windowSize >= 1; windowSize--) {
                String candidate = String.join(" ", Arrays.copyOfRange(tokens, i, i + windowSize));
                var entry = catalog.lookup(candidate);
                if (entry.isPresent()) {
                    result.add(entry.get().normalizedName());
                    matchedWindow = windowSize;
                    break;
                }
            }
            i += matchedWindow > 0 ? matchedWindow : 1;
        }
        return List.copyOf(result);
    }
}
