ackage com.neuralhealer.backend.feature.engagement.entity.EngagementAccessRule;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Immutable;

import java.util.UUID;

/**
 * Rules defining what data a doctor can access after an engagement ends.
 * Maps to: engagement_access_rules table
 * 
 * NOTE: This table is READ-ONLY. Rules are pre-seeded in the database.
 */
@Entity
@Table(name = "engagement_access_rules")
@Immutable // Mark as immutable for Hibernate
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementAccessRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_name", nullable = false, unique = true)
    private String ruleName;

    @Column(name = "can_view_all_history")
    private Boolean canViewAllHistory;

    @Column(name = "can_view_current_only")
    private Boolean canViewCurrentOnly;

    @Column(name = "can_view_patient_profile")
    private Boolean canViewPatientProfile;

    @Column(name = "can_modify_notes")
    private Boolean canModifyNotes;

    @Column(name = "can_message_patient")
    private Boolean canMessagePatient;

    // Retention after engagement ends
    @Column(name = "retains_period_access")
    private Boolean retainsPeriodAccess;

    @Column(name = "retains_history_access")
    private Boolean retainsHistoryAccess;

    @Column(name = "retains_no_access")
    private Boolean retainsNoAccess;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
