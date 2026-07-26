package com.dishcover.matching.client;

import com.dishcover.common.text.VietnameseTextNormalizer;
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

    public UserClient(RestClient.Builder builder,
                       @Value("${services.user-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

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
}
