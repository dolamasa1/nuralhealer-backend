package com.neuralhealer.backend.service;

import com.neuralhealer.backend.exception.InvalidVerificationException;
import com.neuralhealer.backend.model.entity.Engagement;
import com.neuralhealer.backend.model.entity.EngagementVerificationToken;
import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.model.enums.TokenStatus;
import com.neuralhealer.backend.model.enums.VerificationType;
import com.neuralhealer.backend.repository.EngagementVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final EngagementVerificationTokenRepository tokenRepository;

    // Constant for token expiry (3 minutes)
    private static final int EXPIRY_MINUTES = 3;

    @Transactional
    public EngagementVerificationToken generateStartToken(Engagement engagement) {
        return createToken(engagement, VerificationType.start);
    }

    @Transactional
    public EngagementVerificationToken generateEndToken(Engagement engagement) {
        return createToken(engagement, VerificationType.end);
    }

    private EngagementVerificationToken createToken(Engagement engagement, VerificationType type) {
        // In a real app, use SecureRandom. For this simple implementation, UUID
        // substring is sufficient.
        String tokenString = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        EngagementVerificationToken token = EngagementVerificationToken.builder()
                .engagement(engagement)
                .token(tokenString)
                .verificationType(type)
                .status(TokenStatus.pending)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES))
                .qrCodeData("neuralhealer://verify/" + type + "/" + tokenString)
                .build();

        return tokenRepository.save(token);
    }

    @Transactional
    public EngagementVerificationToken verifyToken(String tokenString, User user) {
        EngagementVerificationToken token = tokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new InvalidVerificationException("Invalid token"));

        if (token.getStatus() != TokenStatus.pending) {
            throw new InvalidVerificationException("Token is already " + token.getStatus());
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setStatus(TokenStatus.expired);
            tokenRepository.save(token);
            throw new InvalidVerificationException("Token has expired");
        }

        // Validate user participation (must be doctor or patient of the engagement)
        UUID userId = user.getId();
        UUID doctorId = token.getEngagement().getDoctor().getUser().getId();
        UUID patientId = token.getEngagement().getPatient().getUser().getId();

        if (!userId.equals(doctorId) && !userId.equals(patientId)) {
            throw new InvalidVerificationException("User not authorized to verify this engagement");
        }

        // START tokens can ONLY be verified by the PATIENT (doctor creates, patient
        // verifies)
        if (token.getVerificationType() == VerificationType.start && !userId.equals(patientId)) {
            throw new InvalidVerificationException("Only the patient can verify the START token");
        }

        token.setStatus(TokenStatus.verified);
        token.setVerifiedAt(LocalDateTime.now());
        return tokenRepository.save(token);
    }
}
