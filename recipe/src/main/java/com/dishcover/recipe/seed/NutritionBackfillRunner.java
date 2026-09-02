package com.dishcover.recipe.seed;

import com.dishcover.common.nutrition.NutritionIngredientLine;
import com.dishcover.common.nutrition.RecipeNutrition;
import com.dishcover.common.nutrition.RecipeNutritionCalculator;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Backfill servings/nutrition cho công thức đã tồn tại (RecipeSeeder idempotent bỏ qua vì collection
 * không rỗng — không tự tính lại). Dùng 2 lần: (1) migrate 131 công thức seed cũ khi thêm field này
 * lần đầu; (2) tái sử dụng sau này nếu số liệu dinh dưỡng trong ingredient-catalog.json được sửa —
 * rủi ro đã ghi nhận lúc thiết kế (recipe cũ không tự cập nhật theo catalog mới). CHỈ set field còn
 * thiếu (`nutrition` chưa tồn tại), không đụng document khác — chạy bằng profile "backfill-nutrition".
 */
@Component
@Profile("backfill-nutrition")
public class NutritionBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NutritionBackfillRunner.class);
    private static final int DEFAULT_SEED_SERVINGS = 2;

    private final MongoTemplate mongo;
    private final RecipeNutritionCalculator calculator;

    public NutritionBackfillRunner(MongoTemplate mongo, RecipeNutritionCalculator calculator) {
        this.mongo = mongo;
        this.calculator = calculator;
    }

    @Override
    public void run(String... args) {
        var collection = mongo.getCollection("recipes");
        int updated = 0;
        for (Document item : collection.find(new Document("nutrition", new Document("$exists", false)))) {
            Integer servings = item.getInteger("servings", DEFAULT_SEED_SERVINGS);
            RecipeNutrition n = computeNutrition(item, servings);
            Document set = new Document("servings", servings).append("nutrition", toDocument(n));
            collection.updateOne(new Document("_id", item.get("_id")), new Document("$set", set));
            updated++;
        }
        log.info("Backfill nutrition: đã cập nhật {} công thức.", updated);
    }

    private RecipeNutrition computeNutrition(Document item, Integer servings) {
        List<Document> ingredientDocs = item.getList("ingredients", Document.class);
        if (ingredientDocs == null || ingredientDocs.isEmpty()) {
            return RecipeNutrition.EMPTY;
        }
        List<NutritionIngredientLine> lines = ingredientDocs.stream()
                .map(d -> {
                    Object rawAmount = d.get("amount");
                    Double amount = rawAmount instanceof Number n ? n.doubleValue() : null;
                    return new NutritionIngredientLine(d.getString("normalized_name"), amount, d.getString("unit"));
                })
                .toList();
        return calculator.calculate(lines, servings);
    }

    private static Document toDocument(RecipeNutrition n) {
        return new Document("caloriesPerServing", n.caloriesPerServing())
                .append("proteinPerServing", n.proteinPerServing())
                .append("carbPerServing", n.carbPerServing())
                .append("fatPerServing", n.fatPerServing())
                .append("incomplete", n.incomplete());
    }
}
