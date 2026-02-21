package com.neuralhealer.backend.feature.notification.entity;

import com.neuralhealer.backend.shared.entity.User;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private NotificationType type;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.normal;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private NotificationSource source = NotificationSource.system;

    // Stores delivery info e.g., {"sse": true/false, "email": true/false}
    @Type(JsonType.class)
    @Column(name = "delivery_status", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> deliveryStatus = Map.of("sse", false);

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "send_email")
    @Builder.Default
    private Boolean sendEmail = false;

    @Column(name = "sent_at")
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
