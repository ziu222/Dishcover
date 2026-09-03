package com.dishcover.recipe.seed;

import com.dishcover.common.text.VietnameseTextNormalizer;
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
import java.util.List;

/**
 * Nạp bổ sung {@code recipes-spoonacular.json} vào collection {@code recipes} ĐANG CÓ DỮ LIỆU —
 * khác {@link RecipeSeeder} (chỉ chạy khi collection rỗng, dùng cho seed ban đầu). Idempotent theo
 * từng document: chỉ insert {@code _id} chưa tồn tại — an toàn chạy lại khi
 * {@code scripts/fetch-spoonacular.mjs} fetch bổ sung thêm công thức (VD nhóm VĐV/nhanh sau khi
 * quota reset, xem specs/diet-direction-recommendation.md mục 7.6).
 */
@Component
@Profile("seed-spoonacular")
public class SpoonacularSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpoonacularSeedRunner.class);
    private static final String COLLECTION = "recipes";
    private static final String FILE = "classpath:seed/recipes-spoonacular.json";

    private final MongoTemplate mongo;

    public SpoonacularSeedRunner(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(String... args) throws Exception {
        Resource file = new PathMatchingResourcePatternResolver().getResource(FILE);
        String json;
        try (InputStream in = file.getInputStream()) {
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Document wrapper = Document.parse("{\"items\":" + json + "}");
        List<Document> items = wrapper.getList("items", Document.class);

        var collection = mongo.getCollection(COLLECTION);
        int inserted = 0;
        for (Document item : items) {
            if (collection.find(new Document("_id", item.get("_id"))).first() != null) {
                continue; // đã có (chạy lại sau khi fetch bổ sung) -- bỏ qua, không insert trùng
            }
            item.put("normalized_name", VietnameseTextNormalizer.normalize(item.getString("name")));
            collection.insertOne(item);
            inserted++;
        }
        log.info("Đã nạp bổ sung {}/{} công thức Spoonacular (bỏ qua {} đã có sẵn).",
                inserted, items.size(), items.size() - inserted);
    }
}
