package com.dishcover.rag.client;

import com.dishcover.rag.exception.ApiExceptions.UpstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

/** Gọi User Service lấy dietary-preferences thật của user đang chat (specs/rag-service.md mục 1.3). */
@Component
public class RagUserClient {

    private static final Logger log = LoggerFactory.getLogger(RagUserClient.class);

    private final RestClient restClient;

    /**
     * @param builder {@code RestClient.Builder} riêng cho client nội bộ (timeout 3s/5s, xem
     *                {@link com.dishcover.rag.config.RestClientTimeoutConfig})
     * @param baseUrl địa chỉ User Service, cấu hình qua {@code services.user-url}
     */
    public RagUserClient(@Qualifier("internalServiceRestClientBuilder") RestClient.Builder builder,
                          @Value("${services.user-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Lấy danh sách dietary-preferences (dị ứng + chế độ ăn) của user đang chat.
     *
     * @param bearerToken token JWT của user hiện tại, chuyển tiếp sang User Service
     * @return danh sách dietary-preferences thật
     * @throws com.dishcover.rag.exception.ApiExceptions.UpstreamUnavailableException nếu User
     *         Service lỗi/timeout — fail-closed cố ý (xem {@code fallbackDietary}), KHÔNG được
     *         coi im lặng là "không dị ứng gì"
     */
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackDietary")
    public List<DietaryPreferenceDto> getDietaryPreferences(String bearerToken) {
        DietaryPreferenceDto[] prefs = restClient.get()
                .uri("/users/me/dietary-preferences")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(DietaryPreferenceDto[].class);
        return prefs != null ? Arrays.asList(prefs) : List.of();
    }

    /**
     * User down -> KHÔNG được coi như "không dị ứng gì" — fail-open ở đây có thể gợi ý món chứa dị
     * ứng thật của người dùng. Fail-closed: từ chối phục vụ thay vì đoán sai (giống
     * matching/client/UserClient.java, lý do y hệt — specs/rag-service.md mục 3.3).
     */
    @SuppressWarnings("unused")
    private List<DietaryPreferenceDto> fallbackDietary(String bearerToken, Throwable ex) {
        log.warn("User Service không phản hồi được, từ chối phục vụ để đảm bảo an toàn dị ứng: {}", ex.getMessage());
        throw new UpstreamUnavailableException(
                "Không xác nhận được thông tin dị ứng, tạm ngừng chatbot để đảm bảo an toàn", ex);
    }
}
