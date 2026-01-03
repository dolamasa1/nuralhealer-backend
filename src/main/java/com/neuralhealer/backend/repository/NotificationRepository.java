package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Fetch notifications for a user, newest first
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Count unread notifications
    long countByUserIdAndIsReadFalse(UUID userId);
}
