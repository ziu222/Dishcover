package com.dishcover.user.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Gọi các service khác dọn dữ liệu của một user sắp bị xoá.
 *
 * <p>Database-per-service nên không có khoá ngoại nào dọn hộ — mỗi service phải tự xoá phần của
 * mình. Ở đây KHÔNG dùng saga hay bù trừ: mọi endpoint xoá đều idempotent, nên cách xử lý lỗi
 * đúng và rẻ nhất là báo cáo phần nào chưa dọn được rồi để admin bấm xoá lại.
 */
@Component
public class UserDataCleaner {

    private static final Logger log = LoggerFactory.getLogger(UserDataCleaner.class);

    private final RestClient http;
    private final String inventoryUrl;
    private final String notificationUrl;
    private final String internalSecret;

    public UserDataCleaner(RestClient.Builder builder,
                            @Value("${services.inventory-url:http://localhost:8082}") String inventoryUrl,
                            @Value("${services.notification-url:http://localhost:8087}") String notificationUrl,
                            @Value("${internal.service-secret}") String internalSecret) {
        this.http = builder.build();
        this.inventoryUrl = inventoryUrl;
        this.notificationUrl = notificationUrl;
        this.internalSecret = internalSecret;
    }

    /**
     * Dọn dữ liệu của user ở các service khác.
     *
     * @param userId user sắp bị xoá
     * @return danh sách service KHÔNG dọn được; rỗng nghĩa là sạch hết
     */
    public List<String> cleanRemote(Long userId) {
        List<String> failed = new ArrayList<>();
        if (!delete(inventoryUrl + "/internal/users/" + userId + "/items", "inventory")) {
            failed.add("inventory");
        }
        if (!delete(notificationUrl + "/internal/users/" + userId + "/notifications", "notification")) {
            failed.add("notification");
        }
        return failed;
    }

    private boolean delete(String url, String service) {
        try {
            http.delete().uri(url).header("X-Internal-Secret", internalSecret).retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException ex) {
            // Không ném lên: một service lỗi không được chặn việc dọn những service còn lại.
            log.warn("Không dọn được dữ liệu user {} ở {}: {}", userId(url), service, ex.toString());
            return false;
        }
    }

    /** Trích id từ URL chỉ để ghi log cho dễ đọc. */
    private static String userId(String url) {
        String[] parts = url.split("/");
        return parts.length > 3 ? parts[parts.length - 2] : "?";
    }
}
