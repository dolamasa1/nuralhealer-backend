package com.neuralhealer.backend.service;

import com.neuralhealer.backend.model.entity.AiChatMessage;
import com.neuralhealer.backend.model.entity.AiChatSession;
import com.neuralhealer.backend.model.entity.Engagement;
import com.neuralhealer.backend.model.enums.ChatSenderType;
import com.neuralhealer.backend.model.enums.EngagementStatus;
import com.neuralhealer.backend.repository.AiChatMessageRepository;
import com.neuralhealer.backend.repository.AiChatSessionRepository;
import com.neuralhealer.backend.repository.EngagementRepository;
import com.neuralhealer.backend.repository.DoctorProfileRepository;
import com.neuralhealer.backend.model.dto.AuthorizedDoctorResponse;
import com.neuralhealer.backend.model.dto.SessionWithDoctorsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatStorageService {

    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;
    private final EngagementRepository engagementRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    @Transactional
    public UUID getOrCreateSession(UUID patientId) {
        return sessionRepository.findByPatientIdAndIsActiveTrue(patientId)
                .map(AiChatSession::getId)
                .orElseGet(() -> {
                    AiChatSession newSession = AiChatSession.builder()
                            .patientId(patientId)
                            .isActive(true)
                            .startedAt(LocalDateTime.now())
                            .messageCount(0)
                            .sessionType("general")
                            .build();
                    return sessionRepository.save(newSession).getId();
                });
    }

    @Async
    public void saveMessage(UUID sessionId, String sender, String content) {
        long start = System.currentTimeMillis();
        try {
            ChatSenderType type = ChatSenderType.valueOf(sender.toLowerCase());

            AiChatMessage message = AiChatMessage.builder()
                    .sessionId(sessionId)
                    .senderType(type)
                    .content(content)
                    .sentAt(LocalDateTime.now())
                    .contentType("text")
                    .build();

            messageRepository.save(message);

            long duration = System.currentTimeMillis() - start;
            if (duration > 500) {
                log.warn("Slow chat message save: {}ms for session {}", duration, sessionId);
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid sender type '{}' for session {}", sender, sessionId, e);
        } catch (Exception e) {
            log.error("Failed to save chat message for session {}", sessionId, e);
        }
    }

    public List<AiChatSession> getUserSessions(UUID patientId) {
        return sessionRepository.findByPatientIdOrderByStartedAtDesc(patientId);
    }

    public List<AiChatSession> searchSessions(UUID patientId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return getUserSessions(patientId);
        }
        return sessionRepository.searchSessions(patientId, query.trim());
    }

    public List<AiChatMessage> getSessionMessages(UUID sessionId) {
        return messageRepository.findBySessionIdOrderBySentAt(sessionId);
    }

    @Transactional
    public void updateSessionTitle(UUID sessionId, String title) {
        sessionRepository.updateTitle(sessionId, title);
    }

    public List<AuthorizedDoctorResponse> getAuthorizedDoctors(UUID patientUserId) {
        // Get all engagements for this patient
        List<Engagement> engagements = engagementRepository.findByPatientUserId(patientUserId);

        return engagements.stream()
                .filter(engagement -> {
                    // Only include engagements with active or ended status that allow chat viewing
                    if (engagement.getStatus() == EngagementStatus.active) {
                        return true; // Active engagements always have access
                    }

                    // For ended engagements, check if access rules permit history viewing
                    if (engagement.getStatus() == EngagementStatus.ended) {
                        var rule = engagement.getAccessRule();
                        return rule != null && Boolean.TRUE.equals(rule.getRetainsHistoryAccess());
                    }

                    return false;
                })
                .map(engagement -> {
                    var doctor = engagement.getDoctor();
                    var user = doctor.getUser();
                    var rule = engagement.getAccessRule();

                    String accessLevel = engagement.getStatus() == EngagementStatus.active
                            ? "Full Access"
                            : (rule != null && rule.getRuleName() != null ? rule.getRuleName() : "Historical Access");

                    return new AuthorizedDoctorResponse(
                            doctor.getId(),
                            user.getFullName(),
                            doctor.getTitle(),
                            doctor.getSpecialities(),
                            accessLevel,
                            engagement.getStatus() == EngagementStatus.active);
                })
                .toList();
    }

    /**
     * Get all sessions with authorized doctors for a patient in a single response.
     * Optimized to avoid N+1 queries by fetching engagements once.
     */
    public List<SessionWithDoctorsResponse> getSessionsWithAuthorizedDoctors(UUID patientUserId) {
        // Fetch all sessions and engagements in parallel to avoid N+1
        List<AiChatSession> sessions = sessionRepository.findByPatientIdOrderByStartedAtDesc(patientUserId);
        List<Engagement> engagements = engagementRepository.findByPatientUserId(patientUserId);

        // Filter engagements to only those that grant access
        List<Engagement> authorizedEngagements = engagements.stream()
                .filter(engagement -> {
                    if (engagement.getStatus() == EngagementStatus.active) {
                        return true;
                    }
                    if (engagement.getStatus() == EngagementStatus.ended) {
                        var rule = engagement.getAccessRule();
                        return rule != null && Boolean.TRUE.equals(rule.getRetainsHistoryAccess());
                    }
                    return false;
                })
                .toList();

        // Map each session to enriched response with doctors
        return sessions.stream()
                .map(session -> {
                    List<SessionWithDoctorsResponse.DoctorBasicInfo> doctors = authorizedEngagements.stream()
                            .map(engagement -> {
                                var doctor = engagement.getDoctor();
                                var user = doctor.getUser();
                                var rule = engagement.getAccessRule();

                                String accessLevel = engagement.getStatus() == EngagementStatus.active
                                        ? "Full Access"
                                        : (rule != null && rule.getRuleName() != null ? rule.getRuleName()
                                                : "Historical Access");

                                return new SessionWithDoctorsResponse.DoctorBasicInfo(
                                        doctor.getId(),
                                        user.getFullName(),
                                        doctor.getTitle(),
                                        doctor.getSpecialities(),
                                        accessLevel,
                                        engagement.getStatus() == EngagementStatus.active);
                            })
                            .toList();

                    return new SessionWithDoctorsResponse(
                            session.getId(),
                            session.getSessionTitle(),
                            session.getSessionType(),
                            session.getStartedAt(),
                            session.getEndedAt(),
                            session.getIsActive(),
                            session.getMessageCount(),
                            doctors);
                })
                .toList();
    }
}
