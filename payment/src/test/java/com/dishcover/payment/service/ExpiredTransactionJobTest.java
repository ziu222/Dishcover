package com.dishcover.payment.service;

import com.dishcover.payment.config.VnpayProperties;
import com.dishcover.payment.entity.PaymentTransaction;
import com.dishcover.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Job dọn đơn treo. Trọng tâm: ngưỡng phải đúng bằng mốc đã gửi VNPay và chỉ đụng đơn PENDING. */
class ExpiredTransactionJobTest {

    private static final Instant NOW = Instant.parse("2026-08-16T10:00:00Z");

    private PaymentTransactionRepository transactions;
    private ExpiredTransactionJob job;

    @BeforeEach
    void setUp() {
        transactions = mock(PaymentTransactionRepository.class);
        VnpayProperties props = new VnpayProperties("TMN", "secret-du-dai-de-khong-bi-tu-choi", "https://pay",
                "http://return", "2.1.0", "pay", "VND", "vn", 15);
        job = new ExpiredTransactionJob(transactions, props, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * Ngưỡng phải đúng bằng vnpay.expire-minutes — lệch con số này thì sẽ có lúc mình coi đơn hết
     * hạn trong khi VNPay vẫn cho trả tiền, và IPN hợp lệ về tới nơi lại bị chính mình từ chối.
     */
    @Test
    void quetDungMocHetHanDaGuiVnpay() {
        when(transactions.findByStatusAndCreatedAtBefore(anyString(), any())).thenReturn(List.of());

        job.expireStalePending();

        ArgumentCaptor<Instant> threshold = ArgumentCaptor.forClass(Instant.class);
        verify(transactions).findByStatusAndCreatedAtBefore(eq(PaymentTransaction.PENDING), threshold.capture());
        assertEquals(NOW.minus(15, ChronoUnit.MINUTES), threshold.getValue());
    }

    @Test
    void danhDauHetHanCacDonTreo() {
        PaymentTransaction treo = new PaymentTransaction(7L, "PRO_MONTHLY", 49000, "VNPAY");
        when(transactions.findByStatusAndCreatedAtBefore(anyString(), any())).thenReturn(List.of(treo));

        int n = job.expireStalePending();

        assertEquals(1, n);
        assertEquals(PaymentTransaction.EXPIRED, treo.getStatus());
        verify(transactions).saveAll(any());
    }

    /** Không có đơn treo thì đừng ghi DB — quét mỗi 5 phút, ghi rỗng là tốn vô ích. */
    @Test
    void khongCoDonTreoThiKhongGhiDb() {
        when(transactions.findByStatusAndCreatedAtBefore(anyString(), any())).thenReturn(List.of());

        assertEquals(0, job.expireStalePending());
        verify(transactions, never()).saveAll(any());
    }

    /** Chỉ truy vấn đơn PENDING, nên IPN về muộn cho đơn đã SUCCESS không bị job ghi đè. */
    @Test
    void chiDungToiDonPENDING() {
        when(transactions.findByStatusAndCreatedAtBefore(anyString(), any())).thenReturn(List.of());

        job.expireStalePending();

        verify(transactions).findByStatusAndCreatedAtBefore(eq("PENDING"), any());
    }
}
