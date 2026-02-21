package com.neuralhealer.backend.feature.auth.repository;

import com.neuralhealer.backend.feature.auth.entity.EmailVerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing email verification OTP codes.
 */
@Repository
public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, UUID> {

    /**
     * Find valid (unused and not expired) OTP for a user.
     */
    @Query("SELECT o FROM EmailVerificationOtp o WHERE o.user.id = :userId " +
            "AND o.isUsed = false AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    Optional<EmailVerificationOtp> findValidOtpByUserId(@Param("userId") UUID userId,
            @Param("now") LocalDateTime now);

    /**
     * Find OTP by code (for verification).
     */
    @Query("SELECT o FROM EmailVerificationOtp o WHERE o.otpCode = :code AND o.isUsed = false")
    Optional<EmailVerificationOtp> findByOtpCodeAndIsUsedFalse(@Param("code") String code);

    /**
     * Count OTP requests for a user within a time window (rate limiting).
     */
    @Query("SELECT COUNT(o) FROM EmailVerificationOtp o WHERE o.user.id = :userId " +
            "AND o.createdAt > :since")
    long countByUserIdAndCreatedAtAfter(@Param("userId") UUID userId,
            @Param("since") LocalDateTime since);

    /**
     * Invalidate all previous OTPs for a user.
     */
    @Modifying
    @Query("UPDATE EmailVerificationOtp o SET o.isUsed = true WHERE o.user.id = :userId AND o.isUsed = false")
    int invalidateAllForUser(@Param("userId") UUID userId);

    /**
     * Delete expired OTPs (cleanup job).
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationOtp o WHERE o.expiresAt < :before")
    int deleteByExpiresAtBefore(@Param("before") LocalDateTime before);
}
