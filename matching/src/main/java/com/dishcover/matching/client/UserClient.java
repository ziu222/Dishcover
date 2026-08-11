package com.dishcover.matching.client;

import com.dishcover.common.text.VietnameseTextNormalizer;
import com.dishcover.matching.exception.ApiExceptions.UpstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gọi User Service để lấy danh sách dị ứng (type=ALLERGY), quy đổi value tự do (VD "hải sản")
 * sang dạng slug khớp allergenGroup trong catalog (VD "hai_san") — specs/matching-service.md mục 3.1.
 * type=DIET (chay/vegan) KHÔNG xử lý ở đây, ngoài phạm vi stage này.
 */
@Component
public class UserClient {

    private final RestClient restClient;

    /**
     * @param builder builder RestClient dùng chung của service (timeout áp qua {@code RestClientCustomizer})
     * @param baseUrl địa chỉ gốc của User Service, nạp từ {@code services.user-url}
     */
    public UserClient(RestClient.Builder builder,
                       @Value("${services.user-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Lấy danh sách nhóm dị ứng (type=ALLERGY) của người dùng, quy đổi value tự do (VD "hải sản")
     * sang dạng slug khớp allergenGroup trong catalog (VD "hai_san"). Khi User Service lỗi/timeout,
     * circuit breaker gọi fallback NÉM
     * {@link com.dishcover.matching.exception.ApiExceptions.UpstreamUnavailableException}
     * (fail-closed) thay vì trả tập rỗng, vì coi như "không dị ứng gì" có thể gợi ý nhầm món chứa
     * dị ứng thật của người dùng — nguy hiểm hơn việc tạm ngừng gợi ý.
     *
     * @param bearerToken header Authorization ("Bearer &lt;token&gt;") của người dùng đang gọi
     * @return tập slug nhóm dị ứng của người dùng
     * @throws com.dishcover.matching.exception.ApiExceptions.UpstreamUnavailableException nếu User
     *         Service không khả dụng
     */
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetAllergenGroups")
    public Set<String> getAllergenGroups(String bearerToken) {
        DietaryPreferenceDto[] prefs = restClient.get()
                .uri("/users/me/dietary-preferences")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(DietaryPreferenceDto[].class);
        if (prefs == null) {
            return Set.of();
        }
        return Arrays.stream(prefs)
                .filter(p -> "ALLERGY".equals(p.type()))
                .map(p -> VietnameseTextNormalizer.normalize(p.value()).replace(' ', '_'))
                .collect(Collectors.toSet());
    }

    /**
     * User down -> KHÔNG được coi như "không dị ứng gì" (fail-open ở đây có thể gợi ý món chứa dị
     * ứng thật của người dùng — nguy hiểm hơn hẳn việc tạm ngừng gợi ý). Fail-closed: từ chối phục
     * vụ thay vì đoán sai (specs/matching-service.md mục 3.5 review).
     */
    @SuppressWarnings("unused")
    private Set<String> fallbackGetAllergenGroups(String bearerToken, Throwable ex) {
        throw new UpstreamUnavailableException(
                "Không xác nhận được thông tin dị ứng, tạm ngừng gợi ý để đảm bảo an toàn");
    }
}
