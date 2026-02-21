ackage com.neuralhealer.backend.feature.patient.entity.PatientProfile;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Patient-specific profile information.
 * Maps to: patient_profiles table
 * 
 * Contains medical history, demographics, and emergency contact.
 * Links to User via user_id foreign key.
 */
@Entity
@Table(name = "patient_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(name = "emergency_contact", length = 255)
    private String emergencyContact;

    // JSONB column for primary health concerns
    @Type(JsonType.class)
    @Column(name = "primary_health_concerns", columnDefinition = "jsonb")
    private List<String> primaryHealthConcerns;

    // JSONB column for medical history
    @Type(JsonType.class)
    @Column(name = "medical_history", columnDefinition = "jsonb")
    private Map<String, Object> medicalHistory;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
