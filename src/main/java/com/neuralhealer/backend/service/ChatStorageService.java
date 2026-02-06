package com.neuralhealer.backend.service;

import com.neuralhealer.backend.model.entity.AiChatMessage;
import com.neuralhealer.backend.model.entity.AiChatSession;
import com.neuralhealer.backend.model.enums.ChatSenderType;
import com.neuralhealer.backend.repository.AiChatMessageRepository;
import com.neuralhealer.backend.repository.AiChatSessionRepository;
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
}
