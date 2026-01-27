package com.neuralhealer.backend.notification.repository;

import com.neuralhealer.backend.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository
                extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {

        // Count all notifications for a user
        long countByUserId(UUID userId);

        // Find notifications for a user, paginated (already supported by
        // JpaSpecificationExecutor but kept for simple list)
        Page<Notification> findByUserId(UUID userId, Pageable pageable);

        // Find unread count
        long countByUserIdAndIsReadFalse(UUID userId);

        // Stats: Count by priority
        @Query("SELECT n.priority, COUNT(n) FROM Notification n WHERE n.user.id = :userId GROUP BY n.priority")
        List<Object[]> countByPriorityForUser(@Param("userId") UUID userId);

        // Stats: Count by type
        @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.user.id = :userId GROUP BY n.type")
        List<Object[]> countByTypeForUser(@Param("userId") UUID userId);

        // Cleanup: Delete read notifications older than date
        @Modifying
        @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :date")
        int deleteReadOlderThan(@Param("date") LocalDateTime date);

        // Cleanup: Delete unread notifications older than date
        @Modifying
        @Query("DELETE FROM Notification n WHERE n.isRead = false AND n.createdAt < :date")
        int deleteUnreadOlderThan(@Param("date") LocalDateTime date);

        // Cleanup: Delete expired
        @Modifying
        @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
        int deleteExpired(@Param("now") LocalDateTime now);

        // Find notifications that haven't been pushed via SSE yet
        // (delivery_status->>'sse' = 'false')
        @Query(value = "SELECT * FROM notifications n WHERE n.delivery_status->>'sse' = 'false'", nativeQuery = true)
        List<Notification> findUndeliveredSseNotifications();

        // Find undelivered SSE notifications for a specific user
        @Query(value = "SELECT * FROM notifications n WHERE n.user_id = :userId AND n.delivery_status->>'sse' = 'false'", nativeQuery = true)
        List<Notification> findUndeliveredSseNotificationsForUser(@Param("userId") UUID userId);

        // Fetch notifications for a user, newest first (basic list support)
        List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

        /**
         * Finds notifications missed by a user since a specific timestamp.
         * Orders by sent_at asc to ensure correct event sequence during replay.
         */
        @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.sentAt > :lastSeenAt ORDER BY n.sentAt ASC")
        List<Notification> findMissedNotifications(@Param("userId") UUID userId,
                        @Param("lastSeenAt") LocalDateTime lastSeenAt);

        /**
         * Calls the SQL helper to get a localized and context-aware message.
         */
        @Query(value = "SELECT title, message, priority FROM get_notification_message(:templateKey, :userId, :context, CAST(:placeholders AS jsonb))", nativeQuery = true)
        com.neuralhealer.backend.notification.model.LocalizedMessage getLocalizedMessage(
                        @Param("templateKey") String templateKey,
                        @Param("userId") UUID userId,
                        @Param("context") String context,
                        @Param("placeholders") String placeholders);
}
