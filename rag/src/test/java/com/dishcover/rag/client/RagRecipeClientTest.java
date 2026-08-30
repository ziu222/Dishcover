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

/** So khớp tên món + nhận diện tiêu chó danh mục (chay/nhanh/dễ) qua HTTP thật giả lập. */
class RagRecipeClientTest {

    private static final String SUMMARY_PAGE = """
            {"content": [
                {"id": "vn_trung_chien_ca_chua", "name": "Trứng chiên cà chua", "cookTimeMinutes": 15},
                {"id": "au_tiramisu", "name": "Tiramisu", "cookTimeMinutes": 30}
            ]}
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
        holder[0].expect(requestTo("http://recipe-service/recipes?size=500"))
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
        holder[0].expect(requestTo("http://recipe-service/recipes?size=500"))
                .andRespond(withSuccess(SUMMARY_PAGE, MediaType.APPLICATION_JSON));

        List<RecipeDetailDto> result = client.searchByName("Hôm nay nấu gì cũng được, gợi ý đại đi.");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchByCategoryDetectsDifficultyKeyword() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RagRecipeClient client = buildClientReturning(holder);
        holder[0].expect(requestTo("http://recipe-service/recipes?difficulty=EASY"))
                .andRespond(withSuccess("""
                        {"content": [{"id": "vn_trung_chien_ca_chua", "name": "Trứng chiên cà chua", "cookTimeMinutes": 15}]}
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
        holder[0].expect(requestTo("http://recipe-service/recipes?size=500"))
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

    @Test
    void searchByCategoryReturnsNothingWhenNoTriggerKeywordMatches() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RagRecipeClient client = buildClientReturning(holder);

        List<RecipeDetailDto> result = client.searchByCategory("Tôi còn trứng và cà chua, nấu được món gì?");

        assertTrue(result.isEmpty());
        holder[0].verify(); // không gọi HTTP nào cả vì không khớp từ khóa nào
    }
}
