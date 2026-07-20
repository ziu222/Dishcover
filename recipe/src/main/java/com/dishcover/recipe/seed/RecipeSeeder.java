package com.dishcover.recipe.seed;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Seed công thức mẫu vào MongoDB từ các file JSON trong classpath seed/ (CLAUDE.md mục 4).
 * Chỉ chạy khi bật profile "seed" và chỉ nạp nếu collection "recipes" đang rỗng (idempotent).
 * Dùng chung cho cả nguồn tự soạn (recipes-vn.json) và TheMealDB (recipes-themealdb.json — phase 4b).
 */
@Component
@Profile("seed")
public class RecipeSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RecipeSeeder.class);
    private static final String COLLECTION = "recipes";

    private final MongoTemplate mongo;

    public RecipeSeeder(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(String... args) throws Exception {
        long existing = mongo.getCollection(COLLECTION).countDocuments();
        if (existing > 0) {
            log.info("Collection '{}' đã có {} document — bỏ qua seed.", COLLECTION, existing);
            return;
        }

        List<Document> docs = loadAll();
        if (docs.isEmpty()) {
            log.warn("Không tìm thấy công thức nào trong classpath seed/*.json");
            return;
        }
        mongo.getCollection(COLLECTION).insertMany(docs);
        log.info("Đã seed {} công thức vào collection '{}'.", docs.size(), COLLECTION);
    }

    private List<Document> loadAll() throws Exception {
        List<Document> all = new ArrayList<>();
        Resource[] files = new PathMatchingResourcePatternResolver()
                .getResources("classpath:seed/*.json");
        for (Resource file : files) {
            try (InputStream in = file.getInputStream()) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                // Bọc mảng JSON để BSON parse được: {"items": [...]}
                Document wrapper = Document.parse("{\"items\":" + json + "}");
                List<Document> items = wrapper.getList("items", Document.class);
                all.addAll(items);
                log.info("Nạp {} công thức từ {}", items.size(), file.getFilename());
            }
        }
        return all;
    }
}
