package com.dishcover.user.security;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Verify token Cloudflare Turnstile (CAPTCHA cho /auth/login sau {@link LoginAttemptTracker}
 * đủ ngưỡng — xem AuthController). Turnstile là API ngoài nên bọc Circuit Breaker (CLAUDE.md mục 3).
 *
 * <p><b>Fail-open có chủ đích</b>: nếu Turnstile sập/timeout, {@link #fallbackVerify} trả về
 * {@code true} (coi như verify qua) thay vì chặn đăng nhập. CAPTCHA ở đây chỉ là lớp phòng thủ
 * PHỤ — lớp chính là khoá cứng của {@link LoginAttemptTracker} ở ngưỡng cao hơn. Biến sự cố của
 * Cloudflare thành tự-DoS người dùng thật là đánh đổi tệ hơn so với hiếm khi bị brute-force né
 * được CAPTCHA đúng lúc Cloudflare đang sập.
 */
@Component
public class TurnstileClient {

    private final RestClient restClient;
    private final String secretKey;
    private final String verifyUrl;

    /** @param verifyUrl configurable để test trỏ vào cổng không ai lắng nghe, ép lỗi kết nối thật */
    public TurnstileClient(RestClient.Builder builder,
                            @Value("${turnstile.secret-key}") String secretKey,
                            @Value("${turnstile.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}") String verifyUrl) {
        this.restClient = builder.build();
        this.secretKey = secretKey;
        this.verifyUrl = verifyUrl;
    }

    /**
     * Verify token do widget Turnstile phía client sinh ra.
     *
     * @param token    token client gửi lên; thiếu/rỗng coi là verify thất bại NGAY (không phải
     *                 sự cố Turnstile, không fail-open)
     * @param remoteIp IP người dùng, giúp Cloudflare chấm điểm chính xác hơn — có thể null
     * @return true nếu token hợp lệ (hoặc Turnstile sập — xem Javadoc class), false nếu token sai/hết hạn
     */
    @CircuitBreaker(name = "turnstile", fallbackMethod = "fallbackVerify")
    public boolean verify(String token, String remoteIp) {
        if (token == null || token.isBlank()) {
            return false;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token);
        if (remoteIp != null) {
            form.add("remoteip", remoteIp);
        }
        SiteverifyResponse resp = restClient.post()
                .uri(verifyUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(SiteverifyResponse.class);
        return resp != null && resp.success();
    }

    @SuppressWarnings("unused")
    private boolean fallbackVerify(String token, String remoteIp, Throwable ex) {
        return true; // fail-open — xem Javadoc class
    }

    private record SiteverifyResponse(boolean success) {
    }
}
