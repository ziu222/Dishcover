package com.dishcover.common.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chuẩn hóa tên nguyên liệu tiếng Việt về 1 khóa so khớp ASCII ổn định.
 * Dùng chung giữa Inventory/Recipe/Matching/RAG (CLAUDE.md mục 3.3).
 */
public final class VietnameseTextNormalizer {

    // Precompile: normalize() nằm trên hot path (RAG quét toàn bộ catalog mỗi câu hỏi,
    // Matching so khớp mọi nguyên liệu của mọi công thức).
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private VietnameseTextNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        // Locale.ROOT bắt buộc: toLowerCase() theo locale mặc định sẽ biến 'I' thành 'ı'
        // trên máy locale Thổ Nhĩ Kỳ, làm hỏng mọi khóa so khớp.
        // Xử lý 'đ' TRƯỚC khi NFD vì 'đ' không phân rã được thành "d + dấu".
        String s = raw.trim().toLowerCase(Locale.ROOT).replace('đ', 'd');
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = DIACRITICS.matcher(s).replaceAll("");
        s = NON_ALPHANUMERIC.matcher(s).replaceAll(" ");
        return WHITESPACE.matcher(s.trim()).replaceAll(" ");
    }
}
