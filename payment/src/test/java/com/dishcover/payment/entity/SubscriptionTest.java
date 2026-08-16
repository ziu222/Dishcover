package com.dishcover.payment.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hiệu lực gói PRO tính theo mốc thời gian, không phải đọc suông cột status — đây là chỗ quyết
 * định user có được dùng tính năng trả phí hay không, sai là hoặc cho dùng chùa hoặc chặn nhầm
 * người đã trả tiền.
 */
class SubscriptionTest {

    private static final Instant NOW = Instant.parse("2026-08-16T00:00:00Z");

    private Subscription sub(Instant start, Instant end) {
        return new Subscription(1L, "PRO_MONTHLY", start, end, UUID.randomUUID());
    }

    @Test
    void dangTrongHanThiConHieuLuc() {
        Subscription s = sub(NOW.minus(1, ChronoUnit.DAYS), NOW.plus(29, ChronoUnit.DAYS));

        assertTrue(s.isActiveAt(NOW));
    }

    /** Job đổi cờ chưa chạy nhưng đã quá end_at -> phải coi là hết hạn ngay, không cho dùng tiếp. */
    @Test
    void quaEndAtThiHetHieuLucDuStatusVanConACTIVE() {
        Subscription s = sub(NOW.minus(31, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.DAYS));

        assertTrue(Subscription.ACTIVE.equals(s.getStatus()));
        assertFalse(s.isActiveAt(NOW));
    }

    /** Mốc end_at là biên phải (nửa khoảng) — đúng giây hết hạn là hết, không được dôi thêm. */
    @Test
    void dungThoiDiemEndAtLaHetHan() {
        Subscription s = sub(NOW.minus(30, ChronoUnit.DAYS), NOW);

        assertFalse(s.isActiveAt(NOW));
    }

    @Test
    void chuaToiStartAtThiChuaHieuLuc() {
        Subscription s = sub(NOW.plus(1, ChronoUnit.DAYS), NOW.plus(31, ChronoUnit.DAYS));

        assertFalse(s.isActiveAt(NOW));
    }

    @Test
    void daHuyThiHetHieuLucDuConTrongHan() {
        Subscription s = sub(NOW.minus(1, ChronoUnit.DAYS), NOW.plus(29, ChronoUnit.DAYS));
        s.cancel();

        assertFalse(s.isActiveAt(NOW));
    }
}
