package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DoctorProfile entity operations.
 */
@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {

    /**
     * Find doctor profile by user ID.
     */
    Optional<DoctorProfile> findByUserId(UUID userId);

    /**
     * Check if doctor profile exists for user.
     */
    boolean existsByUserId(UUID userId);
}
