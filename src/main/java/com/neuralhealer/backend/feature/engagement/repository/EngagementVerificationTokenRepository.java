package com.neuralhealer.backend.feature.engagement.repository;

import com.neuralhealer.backend.feature.engagement.entity.EngagementVerificationToken;
import com.neuralhealer.backend.feature.engagement.enums.TokenStatus;
import com.neuralhealer.backend.feature.doctor.enums.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EngagementVerificationTokenRepository extends JpaRepository<EngagementVerificationToken, UUID> {

    Optional<EngagementVerificationToken> findByToken(String token);

    Optional<EngagementVerificationToken> findByEngagementIdAndVerificationTypeAndStatus(
            UUID engagementId, VerificationType type, TokenStatus status);

    /**
     * Find all tokens for an engagement of a specific verification type.
     * Used for token refresh logic.
     */
    List<EngagementVerificationToken> findByEngagementIdAndVerificationType(
            UUID engagementId, VerificationType verificationType);
}
