package com.neuralhealer.backend.service;

import com.neuralhealer.backend.exception.*;
import com.neuralhealer.backend.model.dto.*;
import com.neuralhealer.backend.model.entity.*;
import com.neuralhealer.backend.model.enums.*;
import com.neuralhealer.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

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

        // Update Doctor-Patient Relationship: Handled by DB trigger
        // 'update_relationship_status_on_engagement'

        broadcastEngagementStatus(engagement.getId(), "active", "Engagement has been activated");

        // Notify Doctor that Patient has verified/started
        notificationService.notifyUser(
                engagement.getDoctor().getUser().getId(),
                NotificationType.ENGAGEMENT_STARTED,
                "Engagement Activated",
                "Patient " + engagement.getPatient().getUser().getFirstName()
                        + " has verified and started the engagement.");

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

        // Update relationship: Handled by DB trigger
        // 'update_relationship_status_on_engagement'

        broadcastEngagementStatus(engagement.getId(), "ended", "Engagement has been ended");

        // Notify other party
        UUID otherPartyId = engagement.getDoctor().getUser().getId().equals(user.getId())
                ? engagement.getPatient().getUser().getId()
                : engagement.getDoctor().getUser().getId();

        notificationService.notifyUser(
                otherPartyId,
                NotificationType.ENGAGEMENT_ENDED,
                "Engagement Ended",
                "The engagement has been ended.");

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
    public void hardDeleteEngagement(User user, UUID engagementId) {
        Engagement engagement = getEngagementIfAuthorized(engagementId, user);

        // VALIDATION 1: Must be creator doctor
        if (!engagement.getDoctor().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the doctor who created this engagement can delete it");
        }

        // VALIDATION 2: Must be PENDING status
        if (engagement.getStatus() != EngagementStatus.pending) {
            throw new ConflictException(
                    "Can only delete PENDING engagements. Current status: " + engagement.getStatus());
        }

        // Handle doctor_patients record
        DoctorPatient relationship = doctorPatientRepository
                .findByDoctorIdAndPatientId(engagement.getDoctor().getId(), engagement.getPatient().getId())
                .orElse(null);

        if (relationship != null) {
            String relStatus = relationship.getRelationshipStatus();
            if ("INITIAL_PENDING".equals(relStatus)) {
                doctorPatientRepository.delete(relationship);
            } else {
                relationship.setCurrentEngagementId(null);
                doctorPatientRepository.save(relationship);
            }
        }

        engagementRepository.delete(engagement);
    }

    @Transactional
    public EngagementResponse cancelEngagement(User user, UUID engagementId, CancelEngagementRequest request) {
        Engagement engagement = getEngagementIfAuthorized(engagementId, user);

        // Allow both pending and active
        if (engagement.getStatus() != EngagementStatus.pending && engagement.getStatus() != EngagementStatus.active) {
            throw new ConflictException("Cannot cancel engagement in status: " + engagement.getStatus());
        }

        // ACTIVE needs reason
        if (engagement.getStatus() == EngagementStatus.active &&
                (request.reason() == null || request.reason().trim().isEmpty())) {
            throw new IllegalArgumentException("Reason is required for cancelling active engagements");
        }

        // Detect caller
        boolean isDoctor = engagement.getDoctor().getUser().getId().equals(user.getId());
        boolean isPatient = engagement.getPatient().getUser().getId().equals(user.getId());

        if (!isDoctor && !isPatient) {
            throw new ForbiddenException("Not authorized to cancel this engagement");
        }

        CancellationRole cancelledBy = isDoctor ? CancellationRole.DOCTOR : CancellationRole.PATIENT;

        engagement.setStatus(EngagementStatus.cancelled);
        engagement.setEndedBy(user);
        engagement.setEndAt(LocalDateTime.now());
        engagement.setTerminationReason(request.reason());
        engagementRepository.save(engagement);

        // Update Relationship
        DoctorPatient relationship = doctorPatientRepository
                .findByDoctorIdAndPatientId(engagement.getDoctor().getId(), engagement.getPatient().getId())
                .orElse(null);

        if (relationship != null) {
            String newStatus = determineRelationshipStatusAfterCancellation(engagement, relationship, cancelledBy,
                    request.newAccessRule());
            RelationshipStatus relEnum = RelationshipStatus.fromRuleName(newStatus);

            relationship.setRelationshipStatus(newStatus);
            relationship.setIsActive(relEnum.impliesActive());
            relationship.setCurrentEngagementId(null);
            if (!relEnum.impliesActive()) {
                relationship.setRelationshipEndedAt(LocalDateTime.now());
            }
            doctorPatientRepository.save(relationship);
        }

        broadcastEngagementStatus(engagement.getId(), "cancelled",
                "Engagement has been cancelled by " + cancelledBy.name());

        // Notify other party
        UUID recipientId = isDoctor ? engagement.getPatient().getUser().getId()
                : engagement.getDoctor().getUser().getId();
        notificationService.notifyUser(
                recipientId,
                NotificationType.ENGAGEMENT_CANCELLED,
                "Engagement Cancelled",
                (isDoctor ? "Dr. " + engagement.getDoctor().getUser().getLastName()
                        : "Patient " + engagement.getPatient().getUser().getFirstName())
                        + " has cancelled the engagement.");

        return mapToResponse(engagement);
    }

    private String determineRelationshipStatusAfterCancellation(Engagement e, DoctorPatient rp, CancellationRole role,
            String chosenRule) {
        if (e.getStatus() == EngagementStatus.pending) {
            if ("INITIAL_PENDING".equals(rp.getRelationshipStatus())) {
                return "INITIAL_CANCELLED_PENDING";
            }
            return rp.getRelationshipStatus();
        }

        // Active cancellation
        if (role == CancellationRole.PATIENT && chosenRule != null) {
            return chosenRule;
        }

        // Doctor cancels or patient didn't specify: use retention rules
        EngagementAccessRule rule = e.getAccessRule();
        if (rule != null && rule.getRetainsHistoryAccess()) {
            return rule.getRuleName();
        }
        return "NO_ACCESS";
    }

    @Transactional
    public TokenResponse refreshToken(User user, UUID engagementId) {
        Engagement engagement = getEngagementIfAuthorized(engagementId, user);

        if (!engagement.getDoctor().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the creator doctor can refresh tokens");
        }

        if (engagement.getStatus() != EngagementStatus.pending) {
            throw new ConflictException("Can only refresh tokens for PENDING engagements");
        }

        Optional<EngagementVerificationToken> existingToken = tokenRepository
                .findByEngagementIdAndVerificationType(engagementId, VerificationType.start)
                .stream()
                .filter(t -> t.getStatus() == TokenStatus.pending)
                .max(Comparator.comparing(EngagementVerificationToken::getCreatedAt));

        if (existingToken.isPresent() && existingToken.get().getExpiresAt().isAfter(LocalDateTime.now())) {
            return mapTokenToResponse(existingToken.get(), false);
        }

        existingToken.ifPresent(t -> {
            t.setStatus(TokenStatus.expired);
            tokenRepository.save(t);
        });

        EngagementVerificationToken newToken = verificationService.generateStartToken(engagement);
        return mapTokenToResponse(newToken, true);
    }

    @Transactional(readOnly = true)
    public TokenResponse getCurrentToken(User user, UUID engagementId) {
        Engagement engagement = getEngagementIfAuthorized(engagementId, user);

        if (!engagement.getDoctor().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the doctor can retrieve engagement tokens");
        }

        EngagementVerificationToken token = tokenRepository
                .findByEngagementIdAndVerificationType(engagementId, VerificationType.start)
                .stream()
                .filter(t -> t.getStatus() == TokenStatus.pending)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .max(Comparator.comparing(EngagementVerificationToken::getCreatedAt))
                .orElseThrow(() -> new ResourceNotFoundException("No valid token found. Please refresh token."));

        return mapTokenToResponse(token, false);
    }

    private TokenResponse mapTokenToResponse(EngagementVerificationToken token, boolean isNew) {
        return new TokenResponse(
                token.getEngagement().getId(),
                token.getToken(),
                token.getExpiresAt(),
                token.getQrCodeData(),
                isNew);
    }

    // Helper for message service to validate access
    public boolean canAccessEngagement(User user, UUID engagementId) {
        return engagementRepository.findById(engagementId)
                .map(e -> e.getDoctor().getUser().getId().equals(user.getId())
                        || e.getPatient().getUser().getId().equals(user.getId()))
                .orElse(false);
    }

    private void broadcastEngagementStatus(UUID engagementId, String status, String message) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessageType.ENGAGEMENT_STATUS)
                .engagementId(engagementId)
                .content(message)
                .timestamp(LocalDateTime.now())
                .metadata(status)
                .build();

        messagingTemplate.convertAndSend(
                "/topic/engagement/" + engagementId,
                wsMessage);
    }
}
