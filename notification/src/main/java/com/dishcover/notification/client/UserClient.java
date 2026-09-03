package com.dishcover.notification.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Gọi endpoint nội bộ {@code GET /internal/users/{id}} ở User Service để lấy email — xác thực
 * bằng header {@code X-Internal-Secret}, KHÔNG phải JWT (không có JWT nào để forward trong ngữ
 * cảnh Kafka consumer chạy nền, xem specs/notification-service.md mục 5.3).
 */
@Component
public class UserClient {

    private final RestClient restClient;
    private final String internalSecret;

    public UserClient(RestClient.Builder builder,
                       @Value("${services.user-url}") String baseUrl,
                       @Value("${internal.service-secret}") String internalSecret) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory).build();
        this.internalSecret = internalSecret;
    }

    /** @return email của user, hoặc null nếu User Service lỗi/timeout/không tìm thấy (fail-open — email chỉ là best-effort). */
    public String getEmail(Long userId) {
        try {
            InternalUserDto dto = restClient.get()
                    .uri("/internal/users/{id}", userId)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .body(InternalUserDto.class);
            return dto == null ? null : dto.email();
        } catch (RestClientException ex) {
            return null;
        }
    }

    public record InternalUserDto(Long id, String email) {
    }
}
