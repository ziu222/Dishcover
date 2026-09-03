package com.dishcover.notification.service;

import com.dishcover.notification.entity.Notification;
import com.dishcover.notification.repository.NotificationRepository;
import com.dishcover.notification.dto.NotificationDtos.NotificationListResponse;
import com.dishcover.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// ponytail: @DataJpaTest tự thay datasource bằng H2 mặc định (không schema/MODE=PostgreSQL) trừ
// khi tắt bằng Replace.NONE — cần giữ datasource thật (application-test.yml) để test unique
// constraint chạy đúng trên schema notification_service như prod.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired
    NotificationRepository repository;
    @Autowired
    TestEntityManager em;

    NotificationService service() {
        return new NotificationService(repository);
    }

    @Test
    void createIfAbsentInsertsFirstTime() {
        var n = new Notification(1L, "INGREDIENT_EXPIRING_SOON", "t", "m", "/goi-y", 10L);
        var saved = service().createIfAbsent(n);
        assertTrue(saved.isPresent());
    }

    @Test
    void createIfAbsentSkipsDuplicateKey() {
        // ponytail: @DataJpaTest chạy cả test method trong 1 transaction dùng chung. Insert lỗi
        // (vi phạm unique constraint) làm hỏng luôn EntityManager của transaction đó nếu tiếp tục
        // dùng chung (Hibernate "don't flush the Session after an exception occurs") — không phải
        // bug production (createIfAbsent không tự mở transaction, Kafka listener thật gọi nó
        // ngoài transaction nào cả), chỉ là hệ quả của cách @DataJpaTest gói test. Cô lập bằng
        // TestTransaction.start()/end() quanh mỗi lần gọi để mô phỏng đúng thực tế: mỗi lần gọi
        // createIfAbsent là 1 transaction độc lập.
        NotificationService svc = service();
        svc.createIfAbsent(new Notification(1L, "INGREDIENT_EXPIRING_SOON", "t", "m", "/goi-y", 10L));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        var second = svc.createIfAbsent(new Notification(1L, "INGREDIENT_EXPIRING_SOON", "t2", "m2", "/goi-y", 10L));
        assertTrue(second.isEmpty());
        TestTransaction.end();

        TestTransaction.start();
        assertEquals(1, repository.findByUserId(1L, org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void listReturnsUnreadCount() {
        NotificationService svc = service();
        svc.createIfAbsent(new Notification(2L, "INGREDIENT_EXPIRING_SOON", "t", "m", "/goi-y", 20L));
        NotificationListResponse resp = svc.list(2L, false, 0, 10);
        assertEquals(1, resp.items().size());
        assertEquals(1, resp.unreadCount());
    }

    @Test
    void markReadOwnershipCheckThrowsForOtherUser() {
        NotificationService svc = service();
        var saved = svc.createIfAbsent(new Notification(3L, "INGREDIENT_EXPIRING_SOON", "t", "m", "/goi-y", 30L)).get();
        assertThrows(ResourceNotFoundException.class, () -> svc.markRead(999L, saved.getId()));
    }

    @Test
    void markAllReadClearsUnreadCount() {
        NotificationService svc = service();
        svc.createIfAbsent(new Notification(4L, "INGREDIENT_EXPIRING_SOON", "t", "m", "/goi-y", 40L));
        svc.createIfAbsent(new Notification(4L, "INGREDIENT_EXPIRED", "t2", "m2", "/goi-y", 40L));
        svc.markAllRead(4L);
        assertEquals(0, svc.list(4L, false, 0, 10).unreadCount());
    }
}
