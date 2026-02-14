package com.neuralhealer.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Core user entity representing all users in the NeuralHealer platform.
 * Maps to: users table
 * 
 * Implements UserDetails for Spring Security integration.
 * Role is determined by which profile exists (doctor_profiles or
 * patient_profiles).
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(length = 10, nullable = false)
    @Builder.Default
    private String language = "en";

    @Column(length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "mfa_enabled")
    @Builder.Default
    private Boolean mfaEnabled = false;

    @Column(name = "email_verification_required")
    @Builder.Default
    private Boolean emailVerificationRequired = true;

    @Column(name = "email_verification_sent_at")
    private LocalDateTime emailVerificationSentAt;

    @Column(name = "failed_verification_attempts")
    @Builder.Default
    private Integer failedVerificationAttempts = 0;

    @Column(name = "verification_locked_until")
    private LocalDateTime verificationLockedUntil;

    @Column(name = "activity_status", length = 20)
    @Builder.Default
    private String activityStatus = "active";

    @Column(name = "last_activity_check")
    @Builder.Default
    private LocalDateTime lastActivityCheck = LocalDateTime.now();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Relationships - OneToOne with profile tables
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DoctorProfile doctorProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PatientProfile patientProfile;

    // ========================================
    // UserDetails Implementation
    // ========================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Determine role based on which profile exists
        if (doctorProfile != null) {
            return List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"));
        } else if (patientProfile != null) {
            return List.of(new SimpleGrantedAuthority("ROLE_PATIENT"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive != null && isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive != null && isActive && deletedAt == null;
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Get the user's full name.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Check if user is a doctor.
     */
    public boolean isDoctor() {
        return doctorProfile != null;
    }

    /**
     * Check if user is a patient.
     */
    public boolean isPatient() {
        return patientProfile != null;
    }

    /**
     * Check if user's email is verified.
     */
    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    /**
     * Get the user's role based on profile existence.
     */
    public com.neuralhealer.backend.model.enums.UserRole getRole() {
        if (doctorProfile != null) {
            return com.neuralhealer.backend.model.enums.UserRole.DOCTOR;
        } else if (patientProfile != null) {
            return com.neuralhealer.backend.model.enums.UserRole.PATIENT;
        }
        return com.neuralhealer.backend.model.enums.UserRole.ADMIN; // Default or fallback
    }
}
