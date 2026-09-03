package com.dishcover.notification.service;

import com.dishcover.common.exception.ResourceNotFoundException;
import com.dishcover.notification.dto.NotificationDtos.NotificationListResponse;
import com.dishcover.notification.dto.NotificationDtos.NotificationResponse;
import com.dishcover.notification.entity.Notification;
import com.dishcover.notification.repository.NotificationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationInserter inserter;

    public NotificationService(NotificationRepository repository, NotificationInserter inserter) {
        this.repository = repository;
        this.inserter = inserter;
    }

    /**
     * Insert bản ghi mới; nếu đã tồn tại (unique constraint {@code user_id+source_inventory_item_id+type}
     * bị vi phạm) thì trả rỗng — đây là cơ chế dedup thật (DB-level), atomic dưới concurrent access.
     * "Trùng" nghĩa là nguyên liệu này đã báo đúng trạng thái này rồi, không gửi/lưu lần 2.
     * Insert thật đi qua {@link NotificationInserter} (bean riêng, REQUIRES_NEW) — gọi cross-bean
     * để transaction lỗi tự rollback gọn trong chính nó rồi mới ném exception ra ngoài; gọi self
     * (this.xxx() cùng class) sẽ bỏ qua Spring AOP proxy nên @Transactional không có tác dụng
     * (cùng bài học đã áp dụng cho ResilientLlmCaller/ResilientVisionCaller), và nếu không tách
     * transaction riêng thì insert lỗi sẽ đánh rollback-only lên transaction đang dùng chung của
     * caller (Kafka listener xử lý nhiều event, hoặc test dùng chung 1 EntityManager).
     */
    public Optional<Notification> createIfAbsent(Notification n) {
        try {
            return Optional.of(inserter.insert(n));
        } catch (DataIntegrityViolationException ex) {
            return Optional.empty();
        }
    }

    public NotificationListResponse list(Long userId, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
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

/**
 * Bean riêng chỉ để {@link NotificationService#createIfAbsent} gọi cross-bean — xem javadoc ở đó.
 * Package-private, không dùng ở đâu khác.
 */
@Component
class NotificationInserter {

    private final NotificationRepository repository;

    NotificationInserter(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Notification insert(Notification n) {
        return repository.saveAndFlush(n);
    }
}
