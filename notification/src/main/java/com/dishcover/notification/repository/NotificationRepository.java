package com.dishcover.notification.repository;

import com.dishcover.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndRead(Long userId, boolean read, Pageable pageable);

    List<Notification> findByUserIdAndRead(Long userId, boolean read);

    long countByUserIdAndRead(Long userId, boolean read);

    /** Ownership check: chỉ trả về nếu dòng thuộc đúng user — dùng cho PATCH mark-read. */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
}
