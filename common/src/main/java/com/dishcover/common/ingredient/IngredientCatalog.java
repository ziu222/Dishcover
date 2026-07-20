package com.dishcover.common.ingredient;

import com.dishcover.common.text.VietnameseTextNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Từ điển nguyên liệu chuẩn hóa dùng chung (~200 nguyên liệu), load 1 lần từ JSON tĩnh.
 * Xây sẵn alias index để resolve(rawName) → normalized_name canonical ở O(1).
 */
public final class IngredientCatalog {

    private static final String DEFAULT_RESOURCE = "/ingredient-catalog.json";

    private final List<IngredientEntry> entries;
    private final Map<String, IngredientEntry> byNormalizedName;
    private final Map<String, String> aliasIndex;

    public IngredientCatalog(List<IngredientEntry> entries) {
        this.entries = List.copyOf(entries);
        this.byNormalizedName = new HashMap<>();
        this.aliasIndex = new HashMap<>();
        for (IngredientEntry e : this.entries) {
            byNormalizedName.put(e.normalizedName(), e);
            index(e.canonicalName(), e);
            index(e.normalizedName(), e);
            if (e.aliases() != null) {
                for (String alias : e.aliases()) {
                    index(alias, e);
                }
            }
        }
    }

    private void index(String rawKey, IngredientEntry e) {
        String key = VietnameseTextNormalizer.normalize(rawKey);
        if (!key.isEmpty()) {
            aliasIndex.putIfAbsent(key, e.normalizedName());
        }
    }

    /** Load catalog mặc định từ classpath (/ingredient-catalog.json). */
    public static IngredientCatalog loadDefault() {
        return loadFromClasspath(DEFAULT_RESOURCE);
    }

    public static IngredientCatalog loadFromClasspath(String resourcePath) {
        try (InputStream in = IngredientCatalog.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Không tìm thấy catalog: " + resourcePath);
            }
            ObjectMapper mapper = new ObjectMapper();
            List<IngredientEntry> loaded = mapper.readValue(
                    in, mapper.getTypeFactory().constructCollectionType(List.class, IngredientEntry.class));
            return new IngredientCatalog(loaded);
        } catch (IOException ex) {
            throw new IllegalStateException("Lỗi đọc catalog: " + resourcePath, ex);
        }
    }

    /**
     * Chuẩn hóa + tra từ điển. Khớp alias/canonical → trả normalized_name canonical.
     * Không có trong catalog → trả chính chuỗi đã normalize (nguyên liệu lạ vẫn có khóa nhất quán).
     */
    public String resolve(String rawName) {
        String key = VietnameseTextNormalizer.normalize(rawName);
        return aliasIndex.getOrDefault(key, key);
    }

    /** Tra full entry theo normalized_name canonical (để lấy category/shelf_life/allergen). */
    public Optional<IngredientEntry> lookup(String rawName) {
        return Optional.ofNullable(byNormalizedName.get(resolve(rawName)));
    }

    public List<IngredientEntry> entries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }
}
