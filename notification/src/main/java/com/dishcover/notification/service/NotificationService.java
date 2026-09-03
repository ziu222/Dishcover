package com.dishcover.notification.service;

import com.dishcover.common.exception.ResourceNotFoundException;
import com.dishcover.notification.dto.NotificationDtos.NotificationListResponse;
import com.dishcover.notification.dto.NotificationDtos.NotificationResponse;
import com.dishcover.notification.entity.Notification;
import com.dishcover.notification.repository.NotificationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Insert bản ghi mới; nếu đã tồn tại (unique constraint {@code user_id+source_inventory_item_id+type}
     * bị vi phạm) thì trả rỗng — đây là cơ chế dedup thật (DB-level), atomic dưới concurrent access.
     * "Trùng" nghĩa là nguyên liệu này đã báo đúng trạng thái này rồi, không gửi/lưu lần 2.
     */
    public Optional<Notification> createIfAbsent(Notification n) {
        try {
            return Optional.of(repository.saveAndFlush(n));
        } catch (DataIntegrityViolationException ex) {
            return Optional.empty();
        }
    }

    public NotificationListResponse list(Long userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> result = unreadOnly
                ? repository.findByUserIdAndRead(userId, false, pageable)
                : repository.findByUserId(userId, pageable);
        long unreadCount = repository.countByUserIdAndRead(userId, false);
        List<NotificationResponse> items = result.map(NotificationService::toResponse).getContent();
        return new NotificationListResponse(items, unreadCount);
    }

    public void markRead(Long userId, Long id) {
        Notification n = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo id=" + id));
        n.setRead(true);
        repository.save(n);
    }

    public void markAllRead(Long userId) {
        List<Notification> unread = repository.findByUserIdAndRead(userId, false);
        unread.forEach(n -> n.setRead(true));
        repository.saveAll(unread);
    }

    private static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getActionUrl(), n.isRead(), n.getCreatedAt());
    }
}
