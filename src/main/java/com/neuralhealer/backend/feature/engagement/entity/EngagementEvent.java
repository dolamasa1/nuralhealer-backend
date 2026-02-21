ackage com.neuralhealer.backend.feature.engagement.entity.EngagementEvent;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores events/actions that occur during an engagement lifecycle.
 * Maps to: engagement_events table
 */
@Entity
@Table(name = "engagement_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "engagement_id", nullable = false)
    private UUID engagementId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "triggered_at")
    @Builder.Default
    private LocalDateTime triggeredAt = LocalDateTime.now();

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
