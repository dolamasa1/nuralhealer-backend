package com.neuralhealer.backend.service;

import com.neuralhealer.backend.exception.*;
import com.neuralhealer.backend.model.dto.*;
import com.neuralhealer.backend.model.entity.*;
import com.neuralhealer.backend.model.enums.*;
import com.neuralhealer.backend.repository.*;
import lombok.RequiredArgsConstructor;
import com.neuralhealer.backend.notification.entity.NotificationType;
import com.neuralhealer.backend.notification.service.NotificationService;
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
    private final EngagementEventRepository eventRepository;
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;

    private final VerificationService verificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final EngagementMessageService messageService;
    private final com.neuralhealer.backend.integration.gmail.DirectEmailService directEmailService;

    @Transactional
    public StartEngagementResponse initiateEngagement(User initiator, StartEngagementRequest request) {
        UUID doctorUserId, patientUserId;
        String initiatedBy;

        if (initiator.getRole() == UserRole.DOCTOR) {
            doctorUserId = initiator.getId();
            patientUserId = request.patientId();
            if (patientUserId == null) {
                throw new BadRequestException("Patient ID is required for doctor-initiated engagements");
            }
            initiatedBy = "doctor";
        } else if (initiator.getRole() == UserRole.PATIENT) {
            patientUserId = initiator.getId();
            doctorUserId = request.doctorId();
            if (doctorUserId == null) {
                throw new BadRequestException("Doctor ID is required for patient-initiated engagements");
            }
            initiatedBy = "patient";
        } else {
            throw new UnauthorizedException("Only doctors and patients can initiate engagements");
        }

        User targetUser = userRepository.findById(initiator.getRole() == UserRole.DOCTOR ? patientUserId : doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        // Check for existing active engagement
        if (engagementRepository.findActiveEngagement(doctorUserId, patientUserId,
                List.of(EngagementStatus.pending, EngagementStatus.active)).isPresent()) {
            throw new InvalidVerificationException(
                    "An active or pending engagement already exists between these users");
        }

        EngagementAccessRule rule = accessRuleRepository.findByRuleName(request.accessRuleName())
                .orElseThrow(() -> new ResourceNotFoundException("Access rule not found"));

        DoctorProfile doctorProfile = doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        PatientProfile patientProfile = patientProfileRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        Engagement engagement = Engagement.builder()
                .doctor(doctorProfile)
                .patient(patientProfile)
                .accessRule(rule)
                .status(EngagementStatus.pending)
                .initiatedBy(initiatedBy)
                .notes(request.message())
                .build();

        engagement = engagementRepository.save(engagement);

        // Update Doctor-Patient relationship status if first time
        DoctorPatient dp = doctorPatientRepository
                .findByDoctorIdAndPatientId(doctorProfile.getId(), patientProfile.getId())
                .orElseGet(() -> {
                    DoctorPatient newDp = DoctorPatient.builder()
                            .doctorId(doctorProfile.getId())
                            .patientId(patientProfile.getId())
                            .addedAt(LocalDateTime.now())
                            .relationshipStatus(null) // NULL until engagement is activated
                            .isActive(false)
                            .build();
                    return doctorPatientRepository.save(newDp);
                });

        dp.setCurrentEngagementId(engagement.getId());
        doctorPatientRepository.save(dp);

        // Record event
        saveEvent(engagement.getId(), "INITIATED", initiator.getId(),
                "{\"initiatorRole\":\"" + initiator.getRole() + "\", \"initiatedBy\":\"" + initiatedBy + "\"}");

        // Generate 2FA Token
        EngagementVerificationToken token = verificationService.generateStartToken(engagement);

        // Send Email Notification
        if (initiatedBy.equals("doctor")) {
            // Token to patient
            directEmailService.sendEngagementToken(targetUser.getEmail(), targetUser.getFirstName(),
                    initiator.getFirstName(), token.getToken());
        } else {
            // Token to doctor (request from patient)
            directEmailService.sendEngagementRequestFromPatient(
                    targetUser.getEmail(),
                    targetUser.getFirstName(),
                    initiator.getFirstName(),
                    token.getToken(),
                    rule.getRuleName(),
                    request.message(),
                    engagement.getEngagementId());
        }

        return new StartEngagementResponse(
                engagement.getId(),
                engagement.getStatus().name(),
                initiatedBy,
                new StartEngagementResponse.RecipientInfo(targetUser.getRole().name(), targetUser.getEmail()),
                new StartEngagementResponse.VerificationInfo(
                        token.getToken(),
                        token.getQrCodeData(),
                        token.getExpiresAt()));
    }

    @Transactional
    public EngagementResponse verifyStart(User verifier, String tokenString) {
        EngagementVerificationToken token = verificationService.verifyToken(tokenString, verifier);

        if (token.getVerificationType() != VerificationType.start) {
            throw new InvalidVerificationException("Invalid token type for start verification");
        }

        Engagement engagement = token.getEngagement();

        // Directional validation: who should verify?
        if (engagement.getInitiatedBy().equals("doctor")) {
            // Doctor initiated, patient must verify
            if (verifier.getRole() != UserRole.PATIENT) {
                throw new UnauthorizedException("Only patients can verify doctor-initiated engagements");
            }
        } else {
            // Patient initiated, doctor must verify
            if (verifier.getRole() != UserRole.DOCTOR) {
                throw new UnauthorizedException("Only doctors can verify patient-initiated engagements");
            }
        }

        engagement.activate();
        engagementRepository.save(engagement);

        // Record event
        saveEvent(engagement.getId(), "VERIFIED", verifier.getId(), "{\"role\":\"" + verifier.getRole() + "\"}");

        // Explicitly update relationship to ensure started_at immutability
        DoctorPatient dp = doctorPatientRepository
                .findByDoctorIdAndPatientId(engagement.getDoctor().getId(), engagement.getPatient().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Relationship not found"));

        dp.setRelationshipStatus(engagement.getAccessRule().getRuleName());
        dp.setCurrentEngagementId(engagement.getId());
        dp.setIsActive(true);
        if (dp.getRelationshipStartedAt() == null) {
            dp.setRelationshipStartedAt(LocalDateTime.now());
        }
        dp.setRelationshipEndedAt(null);
        doctorPatientRepository.save(dp);

        // Record System Message
        messageService.sendSystemMessage(engagement,
                "✅ Engagement activated. Access level: " + engagement.getAccessRule().getRuleName());

        broadcastEngagementStatus(engagement.getId(), "active", "Engagement has been activated");

        // Notify Initiator that Verifier has verified/started
        User initiator = engagement.getInitiatedBy().equals("doctor")
                ? engagement.getDoctor().getUser()
                : engagement.getPatient().getUser();

        notificationService.notifyUser(
                initiator.getId(),
                NotificationType.ENGAGEMENT_STARTED,
                "Engagement Activated",
                verifier.getFirstName() + " has verified and started the engagement.");

        // Send Email Confirmation to Initiator
        if (engagement.getInitiatedBy().equals("patient")) {
            // Patient initiated -> send to patient that doctor accepted
            directEmailService.sendEngagementActivatedToPatient(
                    engagement.getPatient().getUser().getEmail(),
                    engagement.getPatient().getUser().getFirstName(),
                    engagement.getDoctor().getUser().getFirstName(),
                    engagement.getEngagementId(),
                    engagement.getAccessRule().getRuleName(),
                    LocalDateTime.now().toString());
        }

        return mapToResponse(engagement);
    }

    @Transactional
    public StartEngagementResponse requestEnd(User user, UUID engagementId, String reason) {
        Engagement engagement = getEngagementIfAuthorized(engagementId, user);

        if (engagement.getStatus() != EngagementStatus.active && engagement.getStatus() != EngagementStatus.pending) {
            throw new InvalidVerificationException("Only active or pending engagements can be ended");
        }

        EngagementVerificationToken token = verificationService.generateEndToken(engagement);

        StartEngagementResponse response = new StartEngagementResponse(
                engagement.getId(),
                engagement.getStatus().name(),
                engagement.getInitiatedBy(),
                new StartEngagementResponse.RecipientInfo(
                        user.getId().equals(engagement.getDoctor().getUser().getId()) ? "PATIENT" : "DOCTOR",
                        user.getId().equals(engagement.getDoctor().getUser().getId())
                                ? engagement.getPatient().getUser().getEmail()
                                : engagement.getDoctor().getUser().getEmail()),
                new StartEngagementResponse.VerificationInfo(
                        token.getToken(),
                        token.getQrCodeData(),
                        token.getExpiresAt()));

        // Record event
        saveEvent(engagement.getId(), "END_REQUESTED", user.getId(),
                "{\"reason\":\"" + (reason != null ? reason.replace("\"", "\\\"") : "") + "\"}");

        return response;
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

        // Record event
        saveEvent(engagement.getId(), "ENDED", user.getId(), "{\"reason\":\"Verification completed\"}");

        // Update relationship
        DoctorPatient dp = doctorPatientRepository
                .findByDoctorIdAndPatientId(engagement.getDoctor().getId(), engagement.getPatient().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Relationship not found"));

        // Retention logic: if NO_ACCESS or similar, set ended_at
        RelationshipStatus finalStatus = RelationshipStatus.fromRuleName(engagement.getAccessRule().getRuleName());
        if (finalStatus == RelationshipStatus.CURRENT_ENGAGEMENT_ACCESS ||
                finalStatus == RelationshipStatus.LIMITED_ENGAGEMENT_ACCESS) {
            dp.setRelationshipStatus("NO_ACCESS");
            dp.setIsActive(false);
            dp.setRelationshipEndedAt(LocalDateTime.now());
        } else {
            dp.setRelationshipStatus(finalStatus.getRuleName());
            dp.setIsActive(finalStatus.impliesActive());
        }
        dp.setCurrentEngagementId(null);
        doctorPatientRepository.save(dp);

        broadcastEngagementStatus(engagement.getId(), "ended", "Engagement has been ended");

        // Record system message
        messageService.sendSystemMessage(engagement, "Engagement has been ended by " + user.getFirstName());

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
        if (engagementId == null) {
            throw new BadRequestException("Engagement ID is required");
        }
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
                e.getInitiatedBy(),
                new EngagementResponse.UserSummary(docUser.getId(), docUser.getFirstName(),
                        docUser.getLastName(), docUser.getEmail()),
                new EngagementResponse.UserSummary(patUser.getId(), patUser.getFirstName(),
                        patUser.getLastName(), patUser.getEmail()),
                e.getAccessRule() != null ? e.getAccessRule().getRuleName() : null,
                e.getStartAt(),
                e.getEndAt(),
                e.getTerminationReason());
    }

    @Transactional
    public void hardDeleteEngagement(User user, UUID engagementId) {
        Engagement engagement = getEngagementIfAuthorized(engagementId, user);

        // VALIDATION 1: Must be doctor OR patient in the engagement
        boolean isDoctor = engagement.getDoctor().getUser().getId().equals(user.getId());
        boolean isPatient = engagement.getPatient().getUser().getId().equals(user.getId());

        if (!isDoctor && !isPatient) {
            throw new ForbiddenException("Only participants can delete this engagement");
        }

        // Delete works on ANY status - complete removal from database

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

        // VALIDATION: Patient MUST provide newAccessRule if engagement is active
        if (engagement.getStatus() == EngagementStatus.active && isPatient) {
            if (request.newAccessRule() == null || request.newAccessRule().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Patients must provide a newAccessRule when cancelling an active engagement");
            }
        }

        // VALIDATION: Doctor CANNOT provide newAccessRule (access is always revoked)
        if (isDoctor && request.newAccessRule() != null && !request.newAccessRule().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Doctors cannot specify a record access rule; access is always revoked upon doctor cancellation");
        }

        CancellationRole cancelledBy = isDoctor ? CancellationRole.DOCTOR : CancellationRole.PATIENT;

        engagement.setStatus(EngagementStatus.cancelled);
        engagement.setEndedBy(user);
        engagement.setEndAt(LocalDateTime.now());
        engagement.setTerminationReason(request.reason());
        engagementRepository.save(engagement);

        // Store event in engagement_events table
        EngagementEvent event = EngagementEvent.builder()
                .engagementId(engagementId)
                .eventType("CANCELLED")
                .triggeredBy(user.getId())
                .payload("{\"reason\":\"" + (request.reason() != null ? request.reason().replace("\"", "\\\"") : "")
                        + "\",\"cancelledBy\":\"" + cancelledBy.name() + "\"}")
                .build();
        eventRepository.save(event);

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

        // Notify BOTH parties
        String cancellerName = isDoctor
                ? "Dr. " + engagement.getDoctor().getUser().getLastName()
                : "Patient " + engagement.getPatient().getUser().getFirstName();

        // Notify doctor
        notificationService.notifyUser(
                engagement.getDoctor().getUser().getId(),
                NotificationType.ENGAGEMENT_CANCELLED,
                "Engagement Cancelled",
                cancellerName + " has cancelled the engagement.");

        // Notify patient
        notificationService.notifyUser(
                engagement.getPatient().getUser().getId(),
                NotificationType.ENGAGEMENT_CANCELLED,
                "Engagement Cancelled",
                cancellerName + " has cancelled the engagement.");

        // Record system message
        String actorLabel = isDoctor ? "Dr. " + engagement.getDoctor().getUser().getLastName()
                : "Patient " + engagement.getPatient().getUser().getFirstName();
        String systemMessageContent = "🚫 " + actorLabel + " cancelled the "
                + (engagement.getStatus() == EngagementStatus.active ? "active" : "pending") + " engagement.";
        if (request.reason() != null && !request.reason().trim().isEmpty()) {
            systemMessageContent += "\nReason: " + request.reason();
        }
        if (engagement.getStatus() == EngagementStatus.active) {
            systemMessageContent += "\nDoctor access set to: " + (isDoctor ? "NO_ACCESS" : request.newAccessRule());
        }
        messageService.sendSystemMessage(engagement, systemMessageContent);

        return mapToResponse(engagement);
    }

    private String determineRelationshipStatusAfterCancellation(Engagement e, DoctorPatient rp, CancellationRole role,
            String chosenRule) {
        // Pending cancellation
        if (e.getStatus() == EngagementStatus.pending) {
            if ("INITIAL_PENDING".equals(rp.getRelationshipStatus())) {
                return "INITIAL_CANCELLED_PENDING";
            }
            return rp.getRelationshipStatus();
        }

        // Active cancellation by DOCTOR = always NO_ACCESS
        if (role == CancellationRole.DOCTOR) {
            return "NO_ACCESS";
        }

        // Active cancellation by PATIENT = use their choice, otherwise NO_ACCESS
        if (role == CancellationRole.PATIENT && chosenRule != null && !chosenRule.trim().isEmpty()) {
            return chosenRule;
        }
        return "NO_ACCESS";
    }

    @Transactional
    public TokenResponse refreshToken(User user, UUID engagementId) {
        Engagement engagement = getEngagementIfAuthorized(engagementId, user);

        // Check if caller is the initiator
        boolean isInitiator = (engagement.getInitiatedBy().equals("doctor")
                && engagement.getDoctor().getUser().getId().equals(user.getId()))
                || (engagement.getInitiatedBy().equals("patient")
                        && engagement.getPatient().getUser().getId().equals(user.getId()));

        if (!isInitiator) {
            throw new ForbiddenException("Only the initiator can refresh tokens");
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

        // Only initiator should be able to see the token to share it (if not already
        // sent via email)
        boolean isInitiator = (engagement.getInitiatedBy().equals("doctor")
                && engagement.getDoctor().getUser().getId().equals(user.getId()))
                || (engagement.getInitiatedBy().equals("patient")
                        && engagement.getPatient().getUser().getId().equals(user.getId()));

        if (!isInitiator) {
            throw new ForbiddenException("Only the initiator can retrieve engagement tokens");
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

    private void saveEvent(UUID engagementId, String type, UUID userId, String payload) {
        EngagementEvent event = EngagementEvent.builder()
                .engagementId(engagementId)
                .eventType(type)
                .triggeredBy(userId)
                .payload(payload)
                .build();
        eventRepository.save(event);
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
