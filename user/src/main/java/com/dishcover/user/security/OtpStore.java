package com.dishcover.user.security;

import com.dishcover.user.exception.ApiExceptions.TooSoonException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sinh/verify mã OTP 6 số cho luồng xác thực email lúc đăng ký, in-memory + TTL — cùng pattern
 * {@link LoginAttemptTracker} đang dùng, không cần Redis/bảng DB cho dữ liệu sống vài phút
 * (docs/specs/email-otp-verification.md mục 1).
 */
@Component
public class OtpStore {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private static final Duration DEFAULT_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_WRONG_ATTEMPTS = 5;

    private final Duration ttl;
    private final Duration cooldown;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Entry> codes = new ConcurrentHashMap<>();

    private static final class Entry {
        final String code;
        final Instant expiresAt;
        final Instant cooldownUntil;
        final AtomicInteger wrongAttempts = new AtomicInteger(0);

        Entry(String code, Instant expiresAt, Instant cooldownUntil) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.cooldownUntil = cooldownUntil;
        }
    }

    public enum VerifyResult {
        OK, WRONG, EXPIRED, NOT_FOUND
    }

    /** Bean thật dùng cho Spring — TTL/cooldown theo giá trị mặc định của tính năng. */
    public OtpStore() {
        this(DEFAULT_TTL, DEFAULT_COOLDOWN);
    }

    /** Test-only: cho phép TTL/cooldown nhỏ để không phải chờ thật trong test. */
    public OtpStore(Duration ttl, Duration cooldown) {
        this.ttl = ttl;
        this.cooldown = cooldown;
    }

    /**
     * Sinh mã OTP mới cho email, ghi đè mã cũ nếu có.
     *
     * @param email email đã chuẩn hoá (lowercase/trim)
     * @return mã 6 số vừa sinh, dùng để gửi qua {@code EmailSender}
     * @throws TooSoonException nếu còn trong cửa sổ cooldown của lần gửi trước
     */
    public String issue(String email) {
        Entry existing = codes.get(email);
        if (existing != null && Instant.now().isBefore(existing.cooldownUntil)) {
            long secondsRemaining = Duration.between(Instant.now(), existing.cooldownUntil).getSeconds() + 1;
            throw new TooSoonException(secondsRemaining);
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        Instant now = Instant.now();
        codes.put(email, new Entry(code, now.plus(ttl), now.plus(cooldown)));
        return code;
    }

    /**
     * Kiểm tra mã OTP người dùng nhập.
     *
     * @param email email đã chuẩn hoá
     * @param code  mã 6 số người dùng nhập
     * @return kết quả — {@code WRONG} tự tăng bộ đếm sai, tới {@value #MAX_WRONG_ATTEMPTS} thì
     *         tự xoá entry (lần verify KẾ TIẾP sẽ trả {@code NOT_FOUND})
     */
    public VerifyResult verify(String email, String code) {
        Entry entry = codes.get(email);
        if (entry == null) {
            return VerifyResult.NOT_FOUND;
        }
        if (Instant.now().isAfter(entry.expiresAt)) {
            codes.remove(email);
            return VerifyResult.EXPIRED;
        }
        if (!entry.code.equals(code)) {
            if (entry.wrongAttempts.incrementAndGet() >= MAX_WRONG_ATTEMPTS) {
                codes.remove(email);
            }
            return VerifyResult.WRONG;
        }
        return VerifyResult.OK;
    }

    /** Xoá entry sau khi verify thành công — không để mã cũ dùng lại được lần nữa. */
    public void clear(String email) {
        codes.remove(email);
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    void evictExpired() {
        Instant now = Instant.now();
        codes.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
    }
}
