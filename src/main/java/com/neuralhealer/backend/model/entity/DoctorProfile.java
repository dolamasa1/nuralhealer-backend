package com.neuralhealer.backend.model.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Doctor-specific profile information.
 * Maps to: doctor_profiles table
 * 
 * Contains professional details, credentials, and verification status.
 * Links to User via user_id foreign key.
 */
@Entity
@Table(name = "doctor_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String bio;

    // JSONB column for specialities array
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> specialities;

    @Column(name = "experience_years")
    private Integer experienceYears;

    // JSONB column for certificates array
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> certificates;

    @Column(name = "location_city", length = 100)
    private String locationCity;

    @Column(name = "location_country", length = 100)
    private String locationCountry;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    // JSONB column for verification data
    @Type(JsonType.class)
    @Column(name = "verification_data", columnDefinition = "jsonb")
    private Map<String, Object> verificationData;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
