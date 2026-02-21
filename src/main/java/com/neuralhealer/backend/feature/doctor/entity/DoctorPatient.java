ackage com.neuralhealer.backend.feature.doctor.entity.DoctorPatient;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.UUID;

/**
 * Junction table tracking the relationship between a doctor and a patient.
 * Maps to: doctor_patients table
 * 
 * Key business rule: Only ONE active engagement per pair allowed.
 */
@Entity
@Table(name = "doctor_patients")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(DoctorPatientId.class) // Composite Key
public class DoctorPatient {

    @Id
    @Column(name = "doctor_id")
    private UUID doctorId;

    @Id
    @Column(name = "patient_id")
    private UUID patientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", insertable = false, updatable = false)
    private DoctorProfile doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", insertable = false, updatable = false)
    private PatientProfile patient;

    @Column(name = "relationship_status", length = 50)
    private String relationshipStatus;

    @Column(name = "is_active")
    private Boolean isActive;

    // Tracks the currently active engagement ID (if any)
    @Column(name = "current_engagement_id")
    private UUID currentEngagementId;

    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;

    @Column(name = "relationship_started_at")
    private LocalDateTime relationshipStartedAt;

    @Column(name = "relationship_ended_at")
    private LocalDateTime relationshipEndedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
