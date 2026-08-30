package com.dishcover.rag.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** So khớp tên món + nhận diện tiêu chí danh mục (chay/nhanh/dễ) qua HTTP thật giả lập. */
class RagRecipeClientTest {

    private static final String SUMMARY_PAGE = """
            {"content": [
                {"id": "vn_trung_chien_ca_chua", "name": "Trứng chiên cà chua", "cookTimeMinutes": 15},
                {"id": "au_tiramisu", "name": "Tiramisu", "cookTimeMinutes": 30}
            ], "last": true}
            """;
    private static final String DETAIL = """
            {"id": "au_tiramisu", "name": "Tiramisu", "slug": "tiramisu",
             "ingredients": [{"name": "Bánh quy"}, {"name": "Cà phê"}]}
            """;

    private RagRecipeClient buildClientReturning(MockRestServiceServer[] serverOut) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        serverOut[0] = server;
        return new RagRecipeClient(builder, "http://recipe-service");
    }

    @Test
    void searchByNameMatchesRecipeNameAsSubstringOfQuestion() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RagRecipeClient client = buildClientReturning(holder);
        holder[0].expect(requestTo("http://recipe-service/recipes?size=100&page=0"))
                .andRespond(withSuccess(SUMMARY_PAGE, MediaType.APPLICATION_JSON));
        holder[0].expect(requestTo("http://recipe-service/recipes/au_tiramisu"))
                .andRespond(withSuccess(DETAIL, MediaType.APPLICATION_JSON));

        List<RecipeDetailDto> result = client.searchByName("Tiramisu nấu ra sao?");

        assertEquals(1, result.size());
        assertEquals("au_tiramisu", result.get(0).id());
    }

    @Test
    void searchByNameFindsNothingWhenQuestionIsUnrelated() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RagRecipeClient client = buildClientReturning(holder);
        holder[0].expect(requestTo("http://recipe-service/recipes?size=100&page=0"))
                .andRespond(withSuccess(SUMMARY_PAGE, MediaType.APPLICATION_JSON));

        List<RecipeDetailDto> result = client.searchByName("Hôm nay nấu gì cũng được, gợi ý đại đi.");

        assertTrue(result.isEmpty());
    }

    /**
     * Regression: Recipe Service giới hạn cứng max-page-size=100 (application.yml). Nếu chỉ gọi
     * 1 trang, công thức nằm ở trang 2 trở đi sẽ ÂM THẦM không bao giờ khớp được — bug thật tìm
     * được lúc live-verify (131 công thức, trang đầu chỉ có 100).
     */
    @Test
    void fetchSummariesLoopsThroughAllPagesUntilLast() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RagRecipeClient client = buildClientReturning(holder);
        holder[0].expect(requestTo("http://recipe-service/recipes?size=100&page=0"))
                .andRespond(withSuccess("""
                        {"content": [{"id": "page0_item", "name": "Món trang một", "cookTimeMinutes": 10}], "last": false}
                        """, MediaType.APPLICATION_JSON));
        holder[0].expect(requestTo("http://recipe-service/recipes?size=100&page=1"))
                .andRespond(withSuccess("""
                        {"content": [{"id": "page1_item", "name": "Phở bò", "cookTimeMinutes": 30}], "last": true}
                        """, MediaType.APPLICATION_JSON));
        holder[0].expect(requestTo("http://recipe-service/recipes/page1_item"))
                .andRespond(withSuccess("""
                        {"id": "page1_item", "name": "Phở bò", "slug": "pho-bo", "ingredients": []}
                        """, MediaType.APPLICATION_JSON));

        List<RecipeDetailDto> result = client.searchByName("Cho tôi công thức Phở bò được không?");

        assertEquals(1, result.size());
        assertEquals("page1_item", result.get(0).id());
    }

    @Test
    void searchByCategoryDetectsDifficultyKeyword() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RagRecipeClient client = buildClientReturning(holder);
        holder[0].expect(requestTo("http://recipe-service/recipes?size=100&difficulty=EASY&page=0"))
                .andRespond(withSuccess("""
                        {"content": [{"id": "vn_trung_chien_ca_chua", "name": "Trứng chiên cà chua", "cookTimeMinutes": 15}], "last": true}
                        """, MediaType.APPLICATION_JSON));
        holder[0].expect(requestTo("http://recipe-service/recipes/vn_trung_chien_ca_chua"))
                .andRespond(withSuccess("""
                        {"id": "vn_trung_chien_ca_chua", "name": "Trứng chiên cà chua", "slug": "trung-chien-ca-chua",
                         "ingredients": [{"name": "Trứng gà"}]}
                        """, MediaType.APPLICATION_JSON));

        List<RecipeDetailDto> result = client.searchByCategory("Gợi ý món dễ làm cho người mới tập nấu ăn.");

        assertEquals(1, result.size());
        assertEquals("vn_trung_chien_ca_chua", result.get(0).id());
    }

    @Test
    void searchByCategoryExtractsExplicitMinutesThreshold() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RagRecipeClient client = buildClientReturning(holder);
        holder[0].expect(requestTo("http://recipe-service/recipes?size=100&page=0"))
                .andRespond(withSuccess(SUMMARY_PAGE, MediaType.APPLICATION_JSON));
        holder[0].expect(requestTo("http://recipe-service/recipes/vn_trung_chien_ca_chua"))
                .andRespond(withSuccess("""
                        {"id": "vn_trung_chien_ca_chua", "name": "Trứng chiên cà chua", "slug": "trung-chien-ca-chua",
                         "ingredients": [{"name": "Trứng gà"}]}
                        """, MediaType.APPLICATION_JSON));

        // Tiramisu (30 phút) không dưới ngưỡng 20 -> chỉ trứng chiên cà chua (15 phút) được chọn.
        List<RecipeDetailDto> result = client.searchByCategory("Món nào nấu nhanh dưới 20 phút?");

        assertEquals(1, result.size());
        assertEquals("vn_trung_chien_ca_chua", result.get(0).id());
    }

    /**
     * Regression bug thật tìm được lúc live-verify (eval/results/bao-cao-tong-hop-danh-gia.md
     * mục 3.3): câu vừa nói "chay" vừa nói "dễ làm" trước đây trộn cả nhánh difficulty=EASY
     * (không lọc ăn kiêng) lẫn tag=chay, kéo nhầm món có hải sản vào chung danh sách với món chay
     * thật -- LLM thấy danh sách tự mâu thuẫn nên từ chối cả loạt. "chay" phải là ràng buộc CỨNG,
     * chặn hẳn nhánh difficulty -- chỉ gọi đúng 1 request tag=chay, không gọi difficulty=EASY.
     */
    @Test
    void chayIsHardConstraintAndSuppressesDifficultyBranchEvenWhenBothKeywordsPresent() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RagRecipeClient client = buildClientReturning(holder);
        holder[0].expect(requestTo("http://recipe-service/recipes?size=100&tag=chay&page=0"))
                .andRespond(withSuccess("""
                        {"content": [{"id": "vn_rau_muong_xao_toi", "name": "Rau muống xào tỏi", "cookTimeMinutes": 10}], "last": true}
                        """, MediaType.APPLICATION_JSON));
        holder[0].expect(requestTo("http://recipe-service/recipes/vn_rau_muong_xao_toi"))
                .andRespond(withSuccess("""
                        {"id": "vn_rau_muong_xao_toi", "name": "Rau muống xào tỏi", "slug": "rau-muong-xao-toi",
                         "ingredients": [{"name": "Rau muống"}, {"name": "Tỏi"}]}
                        """, MediaType.APPLICATION_JSON));

        List<RecipeDetailDto> result = client.searchByCategory("Tôi ăn chay, gợi ý vài món chay dễ làm.");

        assertEquals(1, result.size());
        assertEquals("vn_rau_muong_xao_toi", result.get(0).id());
        holder[0].verify(); // chỉ đúng 2 request (tag=chay + detail) -- không có request difficulty=EASY nào
    }

    @Test
    void searchByCategoryReturnsNothingWhenNoTriggerKeywordMatches() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RagRecipeClient client = buildClientReturning(holder);

        List<RecipeDetailDto> result = client.searchByCategory("Tôi còn trứng và cà chua, nấu được món gì?");

        assertTrue(result.isEmpty());
        holder[0].verify(); // không gọi HTTP nào cả vì không khớp từ khóa nào
    }
}
