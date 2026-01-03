package com.neuralhealer.backend.model.entity;

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

    // Tracks the currently active engagement ID (if any)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_engagement_id")
    private Engagement currentEngagement;

    @Column(name = "last_interaction_at")
    private LocalDateTime lastInteractionAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
