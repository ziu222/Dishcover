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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// ponytail: @DataJpaTest tự thay datasource bằng H2 mặc định (không schema/MODE=PostgreSQL) trừ
// khi tắt bằng Replace.NONE — cần giữ datasource thật (application-test.yml) để test unique
// constraint chạy đúng trên schema notification_service như prod.
// @Import(NotificationService.class): @DataJpaTest không tự nạp bean @Service — cần Spring quản
// lý bean này (không phải `new` tay) để @Transactional(REQUIRES_NEW) trên createIfAbsent thật sự
// chạy qua AOP proxy; thiếu bước này, insert lỗi (dedup) sẽ để lại entity treo và làm hỏng luôn
// EntityManager dùng chung của cả test method (đã verify: lỗi Hibernate AssertionFailure lúc
// build phiên bản `new NotificationService(repository)` tay).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NotificationService.class, NotificationInserter.class})
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired
    NotificationRepository repository;
    @Autowired
    TestEntityManager em;
    @Autowired
    NotificationService service;

    NotificationService service() {
        return service;
    }

    @Test
    void createIfAbsentInsertsFirstTime() {
        var n = new Notification(1L, "INGREDIENT_EXPIRING_SOON", "t", "m", "/goi-y", 10L);
        var saved = service().createIfAbsent(n);
        assertTrue(saved.isPresent());
    }

    @Test
    void createIfAbsentSkipsDuplicateKey() {
        NotificationService svc = service();
        svc.createIfAbsent(new Notification(1L, "INGREDIENT_EXPIRING_SOON", "t", "m", "/goi-y", 10L));
        em.flush();
        em.clear();
        var second = svc.createIfAbsent(new Notification(1L, "INGREDIENT_EXPIRING_SOON", "t2", "m2", "/goi-y", 10L));
        assertTrue(second.isEmpty());
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
