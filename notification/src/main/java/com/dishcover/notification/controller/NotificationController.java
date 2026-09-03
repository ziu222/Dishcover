package com.dishcover.notification.controller;

import com.dishcover.common.security.AuthenticatedUser;
import com.dishcover.notification.dto.NotificationDtos.NotificationListResponse;
import com.dishcover.notification.service.NotificationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** userId luôn lấy từ JWT — không bao giờ nhận từ client (giống Inventory/Matching). */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public NotificationListResponse list(@AuthenticationPrincipal AuthenticatedUser me,
                                          @RequestParam(defaultValue = "false") boolean unreadOnly,
                                          @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                          Pageable pageable) {
        return service.list(me.userId(), unreadOnly, pageable);
    }

    @PatchMapping("/{id}/read")
    public void markRead(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        service.markRead(me.userId(), id);
    }

    @PatchMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal AuthenticatedUser me) {
        service.markAllRead(me.userId());
    }
}
