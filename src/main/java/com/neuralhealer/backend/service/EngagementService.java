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
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;

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
        if (engagementRepository.findActiveEngagement(doctor.getId(), patient.getId(),
                List.of(EngagementStatus.pending, EngagementStatus.active)).isPresent()) {
            throw new InvalidVerificationException("An active or pending engagement already exists for this patient");
        }

        EngagementAccessRule rule = accessRuleRepository.findByRuleName(request.accessRuleName())
                .orElseThrow(() -> new ResourceNotFoundException("Access rule not found"));

        DoctorProfile doctorProfile = doctorProfileRepository.findByUserId(doctor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        PatientProfile patientProfile = patientProfileRepository.findByUserId(patient.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        Engagement engagement = Engagement.builder()
                .doctor(doctorProfile)
                .patient(patientProfile)
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

        if (token.getVerificationType() != VerificationType.start) {
            throw new InvalidVerificationException("Invalid token type for start verification");
        }

        Engagement engagement = token.getEngagement();
        engagement.activate();
        engagementRepository.save(engagement);

        // Update Doctor-Patient Relationship: Handled by DB trigger
        // 'update_relationship_status_on_engagement'

        return mapToResponse(engagement);
    }

    @Transactional
    public StartEngagementResponse requestEnd(User user, UUID engagementId, String reason) {
        Engagement engagement = getEngagementIfAuthorized(engagementId, user);

        if (engagement.getStatus() != EngagementStatus.active && engagement.getStatus() != EngagementStatus.pending) {
            throw new InvalidVerificationException("Only active or pending engagements can be ended");
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

        if (token.getVerificationType() != VerificationType.end) {
            throw new InvalidVerificationException("Invalid token type for end verification");
        }

        Engagement engagement = token.getEngagement();
        engagement.end(user, "Verification completed via token");
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
            engagements = engagementRepository.findByDoctorUserId(user.getId());
        } else {
            engagements = engagementRepository.findByPatientUserId(user.getId());
        }
        return engagements.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private Engagement getEngagementIfAuthorized(UUID engagementId, User user) {
        Engagement engagement = engagementRepository.findById(engagementId)
                .orElseThrow(() -> new EngagementNotFoundException("Engagement not found"));

        if (!engagement.getDoctor().getUser().getId().equals(user.getId()) &&
                !engagement.getPatient().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to view this engagement");
        }
        return engagement;
    }

    private EngagementResponse mapToResponse(Engagement e) {
        User docUser = e.getDoctor().getUser();
        User patUser = e.getPatient().getUser();

        return new EngagementResponse(
                e.getId(),
                e.getEngagementId(),
                e.getStatus().name(),
                new EngagementResponse.UserSummary(docUser.getId(), docUser.getFirstName(),
                        docUser.getLastName(), docUser.getEmail()),
                new EngagementResponse.UserSummary(patUser.getId(), patUser.getFirstName(),
                        patUser.getLastName(), patUser.getEmail()),
                e.getAccessRule() != null ? e.getAccessRule().getRuleName() : null,
                e.getStartAt(),
                e.getEndAt());
    }

    @Transactional
    public void cancelEngagement(User user, UUID engagementId) {
        Engagement engagement = getEngagementIfAuthorized(engagementId, user);

        if (engagement.getStatus() != EngagementStatus.pending) {
            throw new IllegalStateException("Only pending engagements can be cancelled without verification");
        }

        if (!engagement.getDoctor().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Only the initiating doctor can cancel a pending engagement");
        }

        engagement.setStatus(EngagementStatus.cancelled);
        engagement.setEndedBy(user);
        engagement.setTerminationReason("Cancelled by doctor before start");
        engagementRepository.save(engagement);
    }

    // Helper for message service to validate access
    public boolean canAccessEngagement(User user, UUID engagementId) {
        return engagementRepository.findById(engagementId)
                .map(e -> e.getDoctor().getUser().getId().equals(user.getId())
                        || e.getPatient().getUser().getId().equals(user.getId()))
                .orElse(false);
    }
}
