package com.dishcover.user.security;

import com.dishcover.user.exception.ApiExceptions.TooSoonException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.dishcover.user.security.OtpStore.VerifyResult.EXPIRED;
import static com.dishcover.user.security.OtpStore.VerifyResult.NOT_FOUND;
import static com.dishcover.user.security.OtpStore.VerifyResult.OK;
import static com.dishcover.user.security.OtpStore.VerifyResult.WRONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpStoreTest {

    private final OtpStore store = new OtpStore();

    @Test
    void issueReturnsSixDigitCode() {
        String code = store.issue("a@b.com");
        assertEquals(6, code.length());
        assertTrue(code.chars().allMatch(Character::isDigit));
    }

    @Test
    void verifyCorrectCodeReturnsOk() {
        String code = store.issue("a@b.com");
        assertEquals(OK, store.verify("a@b.com", code));
    }

    @Test
    void verifyWrongCodeReturnsWrong() {
        store.issue("a@b.com");
        assertEquals(WRONG, store.verify("a@b.com", "000000"));
    }

    @Test
    void verifyUnknownEmailReturnsNotFound() {
        assertEquals(NOT_FOUND, store.verify("never@seen.com", "123456"));
    }

    @Test
    void fiveWrongAttemptsInvalidatesEntry() {
        String code = store.issue("a@b.com");
        for (int i = 0; i < 5; i++) {
            store.verify("a@b.com", "000000"); // luôn sai
        }
        // Đã sai đủ 5 lần -- entry bị xoá, kể cả gọi lại đúng mã cũ cũng không còn tác dụng
        assertEquals(NOT_FOUND, store.verify("a@b.com", code));
    }

    @Test
    void clearRemovesEntry() {
        store.issue("a@b.com");
        store.clear("a@b.com");
        assertEquals(NOT_FOUND, store.verify("a@b.com", "123456"));
    }

    @Test
    void issueAgainWithinCooldownThrows() {
        store.issue("a@b.com");
        assertThrows(TooSoonException.class, () -> store.issue("a@b.com"));
    }

    @Test
    void expiredCodeReturnsExpired() throws InterruptedException {
        // Constructor test-only: TTL 30ms, cooldown 0 -- hết hạn thật nhanh, không chờ 10 phút.
        OtpStore fastStore = new OtpStore(Duration.ofMillis(30), Duration.ZERO);
        String code = fastStore.issue("a@b.com");
        Thread.sleep(50);
        assertEquals(EXPIRED, fastStore.verify("a@b.com", code));
    }

    @Test
    void issueAgainAfterCooldownElapsedSucceeds() throws InterruptedException {
        OtpStore fastStore = new OtpStore(Duration.ofMinutes(10), Duration.ofMillis(30));
        fastStore.issue("a@b.com");
        Thread.sleep(50);
        String secondCode = fastStore.issue("a@b.com"); // không ném TooSoonException nữa
        assertEquals(6, secondCode.length());
    }
}
