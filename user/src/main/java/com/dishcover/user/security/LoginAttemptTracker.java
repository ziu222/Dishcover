package com.dishcover.user.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Đếm số lần đăng nhập sai liên tiếp theo email, in-memory + TTL — cùng pattern
 * {@code ConversationHistoryStore} (RAG module) đang dùng, không cần Redis ở quy mô đồ án.
 *
 * <p>Ngưỡng 2 tầng (xem AuthController):
 * <ul>
 *   <li>{@link #CAPTCHA_THRESHOLD} lần sai trở lên trong cửa sổ → bắt kèm CAPTCHA hợp lệ</li>
 *   <li>{@link #LOCK_THRESHOLD} lần sai trở lên trong cửa sổ → khoá cứng, từ chối thẳng</li>
 * </ul>
 * Cửa sổ tự hết hạn sau {@link #WINDOW} kể từ lần sai ĐẦU TIÊN — không phải sliding window,
 * chấp nhận nhược điểm burst ở biên cửa sổ (xem thảo luận thiết kế) vì mục tiêu là làm chậm
 * brute-force thông thường, không phải chặn tuyệt đối kẻ tấn công có chủ đích.
 */
@Component
public class LoginAttemptTracker {

    public static final int CAPTCHA_THRESHOLD = 3;
    public static final int LOCK_THRESHOLD = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_EMAILS = 50_000; // chặn phình bộ nhớ nếu bị spam email lạ

    private final ConcurrentHashMap<String, Entry> attempts = new ConcurrentHashMap<>();

    private static final class Entry {
        final AtomicInteger failCount = new AtomicInteger(0);
        final AtomicReference<Instant> windowStart = new AtomicReference<>(Instant.now());
    }

    public enum Status {
        /** Chưa đến ngưỡng nào — cho thử bình thường. */
        OK,
        /** Đã đủ {@link #CAPTCHA_THRESHOLD} lần sai — bắt kèm CAPTCHA hợp lệ mới cho thử tiếp. */
        NEEDS_CAPTCHA,
        /** Đã đủ {@link #LOCK_THRESHOLD} lần sai — khoá cứng, từ chối thẳng không cho thử. */
        LOCKED
    }

    /**
     * Trạng thái hiện tại của email, không làm thay đổi bộ đếm (chỉ đọc, tự dọn nếu cửa sổ
     * đã hết hạn để trạng thái luôn phản ánh đúng — không đợi tới lượt quét định kỳ).
     *
     * @param email email đã chuẩn hoá (lowercase/trim — trùng cách AuthService chuẩn hoá)
     * @return trạng thái hiện tại
     */
    public Status status(String email) {
        Entry entry = attempts.get(email);
        if (entry == null || isExpired(entry)) {
            return Status.OK;
        }
        int fails = entry.failCount.get();
        if (fails >= LOCK_THRESHOLD) {
            return Status.LOCKED;
        }
        if (fails >= CAPTCHA_THRESHOLD) {
            return Status.NEEDS_CAPTCHA;
        }
        return Status.OK;
    }

    /**
     * Ghi nhận 1 lần đăng nhập sai. Cửa sổ cũ đã hết hạn thì bắt đầu cửa sổ mới (không cộng dồn
     * vào lần sai đã quá hạn).
     *
     * @param email email đã chuẩn hoá
     */
    public void recordFailure(String email) {
        if (!attempts.containsKey(email) && attempts.size() >= MAX_TRACKED_EMAILS) {
            return; // đầy -- bỏ qua tracking email MỚI thay vì phình bộ nhớ vô hạn
        }
        Entry entry = attempts.compute(email, (e, existing) ->
                (existing == null || isExpired(existing)) ? new Entry() : existing);
        entry.failCount.incrementAndGet();
    }

    /** Đăng nhập thành công — xoá hẳn lịch sử sai của email, không chờ cửa sổ hết hạn. */
    public void reset(String email) {
        attempts.remove(email);
    }

    private boolean isExpired(Entry entry) {
        return Duration.between(entry.windowStart.get(), Instant.now()).compareTo(WINDOW) > 0;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    void evictExpired() {
        attempts.entrySet().removeIf(e -> isExpired(e.getValue()));
    }
}
