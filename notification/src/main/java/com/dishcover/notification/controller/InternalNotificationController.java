package com.dishcover.notification.controller;

import com.dishcover.common.security.InternalSecretGuard;
import com.dishcover.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Dọn thông báo của một user khi tài khoản bị xoá. Xem InternalInventoryController cùng mục đích. */
@RestController
@RequestMapping("/internal/users")
public class InternalNotificationController {

    private final NotificationRepository repo;
    private final String internalSecret;

    public InternalNotificationController(NotificationRepository repo,
                                           @Value("${internal.service-secret}") String internalSecret) {
        this.repo = repo;
        this.internalSecret = internalSecret;
    }

    /**
     * Xoá toàn bộ thông báo của một user. Idempotent — không có gì để xoá vẫn trả 204.
     *
     * @param id     user cần dọn
     * @param secret header X-Internal-Secret
     */
    @DeleteMapping("/{id}/notifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteAllForUser(@PathVariable Long id,
                                  @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        InternalSecretGuard.verify(internalSecret, secret);
        repo.deleteByUserId(id);
    }
}
