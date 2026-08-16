package com.dishcover.payment.service;

import com.dishcover.payment.entity.Plan;
import com.dishcover.payment.entity.Subscription;
import com.dishcover.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Cấp và tra quyền PRO. Trọng tâm: gia hạn không được ăn mất ngày người dùng đã trả tiền. */
class SubscriptionManagerTest {

    private static final Instant NOW = Instant.parse("2026-08-16T00:00:00Z");
    private static final Plan MONTHLY = new Plan("PRO_MONTHLY", 49000, 30);

    private SubscriptionRepository repo;
    private SubscriptionManager manager;

    @BeforeEach
    void setUp() {
        repo = mock(SubscriptionRepository.class);
        manager = new SubscriptionManager(repo, Clock.fixed(NOW, ZoneOffset.UTC));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void chuaCoGoiThiTinhTuBayGio() {
        when(repo.findByUserIdAndStatusAndEndAtAfter(anyLong(), anyString(), any())).thenReturn(List.of());

        Subscription s = manager.activate(7L, MONTHLY, UUID.randomUUID());

        assertEquals(NOW, s.getStartAt());
        assertEquals(NOW.plus(30, ChronoUnit.DAYS), s.getEndAt());
    }

    /**
     * Gia hạn sớm khi còn hạn: kỳ mới nối tiếp từ mốc hết hạn cũ, KHÔNG tính lại từ bây giờ —
     * tính lại là ăn chặn số ngày người dùng đã trả tiền.
     */
    @Test
    void giaHanKhiConHanThiNoiTiepKhongCatNgayCu() {
        Instant hetHanCu = NOW.plus(10, ChronoUnit.DAYS);
        when(repo.findByUserIdAndStatusAndEndAtAfter(anyLong(), anyString(), any()))
                .thenReturn(List.of(new Subscription(7L, "PRO_MONTHLY", NOW.minus(20, ChronoUnit.DAYS), hetHanCu, UUID.randomUUID())));

        Subscription s = manager.activate(7L, MONTHLY, UUID.randomUUID());

        assertEquals(hetHanCu, s.getStartAt());
        assertEquals(hetHanCu.plus(30, ChronoUnit.DAYS), s.getEndAt());
    }

    /** Lỡ có nhiều bản chồng nhau thì lấy bản hạn xa nhất — chọn nhầm bản sớm là cắt quyền. */
    @Test
    void nhieuBanChongNhauThiLayBanHanXaNhat() {
        Instant gan = NOW.plus(5, ChronoUnit.DAYS);
        Instant xa = NOW.plus(40, ChronoUnit.DAYS);
        when(repo.findByUserIdAndStatusAndEndAtAfter(anyLong(), anyString(), any())).thenReturn(List.of(
                new Subscription(7L, "PRO_MONTHLY", NOW, gan, UUID.randomUUID()),
                new Subscription(7L, "PRO_YEARLY", NOW, xa, UUID.randomUUID())));

        assertEquals(xa, manager.currentActive(7L, NOW).orElseThrow().getEndAt());
    }

    @Test
    void conHanThiIsProTraTrue() {
        when(repo.findByUserIdAndStatusAndEndAtAfter(anyLong(), anyString(), any())).thenReturn(
                List.of(new Subscription(7L, "PRO_MONTHLY", NOW.minus(1, ChronoUnit.DAYS), NOW.plus(29, ChronoUnit.DAYS), UUID.randomUUID())));

        assertTrue(manager.isPro(7L));
    }

    @Test
    void khongCoGoiThiIsProTraFalse() {
        when(repo.findByUserIdAndStatusAndEndAtAfter(anyLong(), anyString(), any())).thenReturn(List.of());

        assertFalse(manager.isPro(7L));
    }

    /** Bản đã hủy lọt vào kết quả truy vấn cũng không được tính là còn quyền. */
    @Test
    void banDaHuyKhongTinhLaConQuyen() {
        Subscription daHuy = new Subscription(7L, "PRO_MONTHLY", NOW.minus(1, ChronoUnit.DAYS), NOW.plus(29, ChronoUnit.DAYS), UUID.randomUUID());
        daHuy.cancel();
        when(repo.findByUserIdAndStatusAndEndAtAfter(anyLong(), anyString(), any())).thenReturn(List.of(daHuy));

        assertFalse(manager.isPro(7L));
    }
}
