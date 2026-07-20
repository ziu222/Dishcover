package com.dishcover.common.text;

import java.text.Normalizer;

/**
 * Chuẩn hóa tên nguyên liệu tiếng Việt về 1 khóa so khớp ASCII ổn định.
 * Dùng chung giữa Inventory/Recipe/Matching/RAG (CLAUDE.md mục 3.3).
 */
public final class VietnameseTextNormalizer {

    private VietnameseTextNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim().toLowerCase()
                .replace('đ', 'd')
                .replace('Đ', 'd');
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.replaceAll("[^a-z0-9\\s]", " ");
        s = s.trim().replaceAll("\\s+", " ");
        return s;
    }
}
