package com.neuralhealer.backend.service;

import com.neuralhealer.backend.exception.EngagementNotFoundException;
import com.neuralhealer.backend.exception.InvalidVerificationException;
import com.neuralhealer.backend.exception.ResourceNotFoundException;
import com.neuralhealer.backend.exception.UnauthorizedException;
import com.neuralhealer.backend.model.dto.EngagementResponse;
import com.neuralhealer.backend.model.dto.StartEngagementRequest;
import com.neuralhealer.backend.model.dto.StartEngagementResponse;
import com.neuralhealer.backend.model.entity.*;
import com.neuralhealer.backend.model.enums.EngagementStatus;
import com.neuralhealer.backend.model.enums.UserRole;
import com.neuralhealer.backend.model.enums.VerificationType;
import com.neuralhealer.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EngagementService {

    private final EngagementRepository engagementRepository;
    private final EngagementVerificationTokenRepository tokenRepository;
    private final EngagementAccessRuleRepository accessRuleRepository;
    private final DoctorPatientRepository doctorPatientRepository;
    private final UserRepository userRepository;

    private final VerificationService verificationService;

    @Transactional
    public StartEngagementResponse initiateEngagement(User doctor, StartEngagementRequest request) {
        if (doctor.getRole() != UserRole.DOCTOR) {
            throw new UnauthorizedException("Only doctors can initiate engagements");
        }

        User patient = userRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        if (patient.getRole() != UserRole.PATIENT) {
            throw new InvalidVerificationException("Selected user is not a patient");
        }

        // Check for existing active engagement
        if (engagementRepository.findActiveEngagement(doctor.getId(), patient.getId()).isPresent()) {
            throw new InvalidVerificationException("An active or pending engagement already exists for this patient");
        }

        EngagementAccessRule rule = accessRuleRepository.findByRuleName(request.accessRuleName())
                .orElseThrow(() -> new ResourceNotFoundException("Access rule not found"));

        Engagement engagement = Engagement.builder()
                .doctor(doctor)
                .patient(patient)
                .accessRule(rule)
                .status(EngagementStatus.pending)
                .build();

        engagement = engagementRepository.save(engagement);

        // Generate 2FA Token
        EngagementVerificationToken token = verificationService.generateStartToken(engagement);

        return new StartEngagementResponse(
                engagement.getId(),
                engagement.getStatus().name(),
                new StartEngagementResponse.VerificationInfo(
                        token.getToken(),
                        token.getQrCodeData(),
                        token.getExpiresAt()));
    }

    @Transactional
    public EngagementResponse verifyStart(User user, String tokenString) {
        EngagementVerificationToken token = verificationService.verifyToken(tokenString, user);

        if (token.getVerificationType() != VerificationType.START) {
            throw new InvalidVerificationException("Invalid token type for start verification");
        }

        Engagement engagement = token.getEngagement();
        engagement.setStatus(EngagementStatus.active);
        engagement.setStartAt(LocalDateTime.now());
        engagementRepository.save(engagement);

        // Update Doctor-Patient Relationship: Handled by DB trigger
        // 'update_relationship_status_on_engagement'

        return mapToResponse(engagement);
    }

    @Transactional
    public StartEngagementResponse requestEnd(User user, UUID engagementId, String reason) {
        Engagement engagement = getEngagementIfAuthorized(engagementId, user);

        if (engagement.getStatus() != EngagementStatus.active) {
            throw new InvalidVerificationException("Engagement is not active");
        }

        EngagementVerificationToken token = verificationService.generateEndToken(engagement);

        return new StartEngagementResponse(
                engagement.getId(),
                engagement.getStatus().name(),
                new StartEngagementResponse.VerificationInfo(
                        token.getToken(),
                        token.getQrCodeData(),
                        token.getExpiresAt()));
    }

    @Transactional
    public EngagementResponse verifyEnd(User user, String tokenString) {
        EngagementVerificationToken token = verificationService.verifyToken(tokenString, user);

        if (token.getVerificationType() != VerificationType.END) {
            throw new InvalidVerificationException("Invalid token type for end verification");
        }

        Engagement engagement = token.getEngagement();
        engagement.setStatus(EngagementStatus.ended); // Or ARCHIVED depending on logic, sticking to ENDED per
                                                      // instructions
        engagement.setEndAt(LocalDateTime.now());
        engagementRepository.save(engagement);

        // Update relationship: Handled by DB trigger
        // 'update_relationship_status_on_engagement'

        return mapToResponse(engagement);
    }

    @Transactional(readOnly = true)
    public EngagementResponse getEngagement(User user, UUID engagementId) {
        Engagement engagement = getEngagementIfAuthorized(engagementId, user);
        return mapToResponse(engagement);
    }

    @Transactional(readOnly = true)
    public List<EngagementResponse> getMyEngagements(User user) {
        List<Engagement> engagements;
        if (user.getRole() == UserRole.DOCTOR) {
            engagements = engagementRepository.findByDoctorId(user.getId());
        } else {
            engagements = engagementRepository.findByPatientId(user.getId());
        }
        return engagements.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private Engagement getEngagementIfAuthorized(UUID engagementId, User user) {
        Engagement engagement = engagementRepository.findById(engagementId)
                .orElseThrow(() -> new EngagementNotFoundException("Engagement not found"));

        if (!engagement.getDoctor().getId().equals(user.getId()) &&
                !engagement.getPatient().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to view this engagement");
        }
        return engagement;
    }

    private EngagementResponse mapToResponse(Engagement e) {
        return new EngagementResponse(
                e.getId(),
                e.getEngagementId(),
                e.getStatus().name(),
                new EngagementResponse.UserSummary(e.getDoctor().getId(), e.getDoctor().getFirstName(),
                        e.getDoctor().getLastName(), e.getDoctor().getEmail()),
                new EngagementResponse.UserSummary(e.getPatient().getId(), e.getPatient().getFirstName(),
                        e.getPatient().getLastName(), e.getPatient().getEmail()),
                e.getAccessRule() != null ? e.getAccessRule().getRuleName() : null,
                e.getStartAt(),
                e.getEndAt());
    }

    // Helper for message service to validate access
    public boolean canAccessEngagement(User user, UUID engagementId) {
        return engagementRepository.findById(engagementId)
                .map(e -> e.getDoctor().getId().equals(user.getId()) || e.getPatient().getId().equals(user.getId()))
                .orElse(false);
    }
}
