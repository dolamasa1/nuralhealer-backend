package com.neuralhealer.backend.notification.repository;

import com.neuralhealer.backend.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Find notifications for a user, paginated
    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    // Find unread count
    long countByUserIdAndIsReadFalse(UUID userId);

    // Find notifications that haven't been pushed via SSE yet
    // (delivery_status->>'sse' = 'false')
    @Query(value = "SELECT * FROM notifications n WHERE n.delivery_status->>'sse' = 'false'", nativeQuery = true)
    List<Notification> findUndeliveredSseNotifications();

    // Find undelivered SSE notifications for a specific user
    @Query(value = "SELECT * FROM notifications n WHERE n.user_id = :userId AND n.delivery_status->>'sse' = 'false'", nativeQuery = true)
    List<Notification> findUndeliveredSseNotificationsForUser(@Param("userId") UUID userId);

    // Fetch notifications for a user, newest first (basic list support)
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
