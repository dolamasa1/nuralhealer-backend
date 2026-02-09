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

    @Column(length = 100)
    private String specialization;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    // JSONB column for certificates array
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> certificates;

    @Column(name = "location_city", length = 100)
    private String locationCity;

    @Column(name = "location_country", length = 100)
    private String locationCountry;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    // Profile picture
    @Column(name = "profile_picture_path", length = 500)
    private String profilePicturePath;

    // Verification and status
    @Column(name = "verification_status", length = 50)
    @Builder.Default
    private String verificationStatus = "unverified";

    @Column(name = "availability_status", length = 50)
    @Builder.Default
    private String availabilityStatus = "offline";

    // Metrics
    @Column
    @Builder.Default
    private Double rating = 0.00;

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "profile_completion_percentage")
    @Builder.Default
    private Integer profileCompletionPercentage = 0;

    // Social media (JSONB)
    @Type(JsonType.class)
    @Column(name = "social_media", columnDefinition = "jsonb")
    private Map<String, String> socialMedia;

    // Pricing
    @Column(name = "consultation_fee")
    private Double consultationFee;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Legacy support for older code calling these methods
    public List<String> getSpecialities() {
        return specialization != null ? List.of(specialization) : List.of();
    }

    public Integer getExperienceYears() {
        return yearsOfExperience;
    }

    public Boolean getIsVerified() {
        return "verified".equalsIgnoreCase(verificationStatus);
    }
}
