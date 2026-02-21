package com.neuralhealer.backend.feature.email.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "message_queues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_type", nullable = false, length = 100)
    private String jobType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "pending";

    @Column(name = "scheduled_at")
    @Builder.Default
    private LocalDateTime scheduledAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "priority", length = 20)
    @Builder.Default
    private String priority = "normal";

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Builder.Default
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
