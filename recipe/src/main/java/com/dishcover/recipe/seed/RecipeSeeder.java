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

    /**
     * @param mongo template MongoDB dùng để đọc/ghi trực tiếp collection {@code recipes}
     */
    public RecipeSeeder(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /**
     * Điểm vào seed dữ liệu, Spring Boot gọi tự động khi context khởi động (profile "seed").
     * Bỏ qua nếu collection {@code recipes} đã có dữ liệu (idempotent), ngược lại nạp toàn bộ
     * file JSON trong {@code classpath:seed/*.json} rồi insert hàng loạt.
     *
     * @param args tham số dòng lệnh (không dùng)
     * @throws Exception nếu đọc file JSON trong classpath thất bại
     */
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

    /**
     * Đọc toàn bộ file JSON trong {@code classpath:seed/*.json}, mỗi file chứa một mảng công thức.
     * Tự tính bù trường {@code normalized_name} ở cấp recipe (dữ liệu seed gốc chỉ có trường này
     * trong {@code ingredients[]}) để tìm kiếm không phân biệt dấu hoạt động nhất quán với dữ liệu
     * tạo mới qua API.
     *
     * @return danh sách document công thức đã sẵn sàng insert vào MongoDB
     * @throws Exception nếu đọc hoặc parse file JSON trong classpath thất bại
     */
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
                // 62 công thức seed không có "normalized_name" ở cấp recipe (chỉ có trong ingredients[]).
                // RecipeService dùng field này để tìm theo tên không phân biệt dấu — tự tính bù ở đây
                // thay vì sửa tay 62 dòng JSON, để dữ liệu cũ vẫn tìm kiếm được như dữ liệu mới tạo qua API.
                for (Document item : items) {
                    item.put("normalized_name", VietnameseTextNormalizer.normalize(item.getString("name")));
                }
                all.addAll(items);
                log.info("Nạp {} công thức từ {}", items.size(), file.getFilename());
            }
        }
        return all;
    }
}
