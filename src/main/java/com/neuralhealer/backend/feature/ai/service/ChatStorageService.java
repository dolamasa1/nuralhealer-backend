ackage com.neuralhealer.backend.feature.ai.service.ChatStorageService;

import com.neuralhealer.backend.feature.ai.entity.AiChatMessage;
import com.neuralhealer.backend.feature.ai.entity.AiChatSession;
import com.neuralhealer.backend.feature.engagement.entity.Engagement;
import com.neuralhealer.backend.feature.ai.enums.ChatSenderType;
import com.neuralhealer.backend.feature.engagement.enums.EngagementStatus;
import com.neuralhealer.backend.feature.ai.repository.AiChatMessageRepository;
import com.neuralhealer.backend.feature.ai.repository.AiChatSessionRepository;
import com.neuralhealer.backend.feature.engagement.repository.EngagementRepository;
import com.neuralhealer.backend.feature.doctor.repository.DoctorProfileRepository;
import com.neuralhealer.backend.feature.doctor.dto.AuthorizedDoctorResponse;
import com.neuralhealer.backend.feature.patient.dto.SessionWithDoctorsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
                .orElseGet(() -> createNewSession(patientId));
    }

    /**
     * Force create a new active session for a patient.
     */
    @Transactional
    public UUID createNewSession(UUID patientId) {
        AiChatSession newSession = AiChatSession.builder()
                .patientId(patientId)
                .isActive(true)
                .startedAt(LocalDateTime.now())
                .messageCount(0)
                .sessionType("general")
                .sessionTitle("New AI Chat")
                .build();
        return sessionRepository.save(newSession).getId();
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
                    .build();

            messageRepository.save(message);

            // Update session: increment count and set title from first patient message
            sessionRepository.findById(sessionId).ifPresent(session -> {
                session.setMessageCount(session.getMessageCount() + 1);

                // Auto-generate title from first patient message
                if (type == ChatSenderType.patient && session.getMessageCount() == 1) { // Changed from 0 to 1 because
                                                                                        // messageCount is incremented
                                                                                        // before this check
                    String autoTitle = generateSessionTitle(content);
                    session.setSessionTitle(autoTitle);
                }

                sessionRepository.save(session);
            });

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

    /**
     * Get session messages with patient ID verification for security.
     * Throws SecurityException if session doesn't belong to patient.
     */
    public List<AiChatMessage> getSessionMessages(UUID sessionId, UUID patientId) {
        AiChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getPatientId().equals(patientId)) {
            throw new SecurityException("Session does not belong to this patient");
        }

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

    /**
     * Generate a meaningful session title from the first user message.
     * Extracts the first sentence or phrase, limited to 50 characters.
     */
    private String generateSessionTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isBlank()) {
            return "New AI Chat";
        }

        // Clean and trim the message
        String cleaned = firstMessage.trim();

        // Extract first sentence (stop at . ! ? or newline)
        int endIndex = Math.min(cleaned.length(), 50);
        for (int i = 0; i < Math.min(cleaned.length(), 50); i++) {
            char c = cleaned.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                endIndex = i;
                break;
            }
        }

        String title = cleaned.substring(0, endIndex).trim();

        // If title is too short or empty, use first 50 chars
        if (title.length() < 5) {
            title = cleaned.substring(0, Math.min(cleaned.length(), 50)).trim();
        }

        // Add ellipsis if truncated
        if (title.length() < cleaned.length() && !title.endsWith(".") && !title.endsWith("!") && !title.endsWith("?")) {
            title += "...";
        }

        return title.isEmpty() ? "New AI Chat" : title;
    }
}
