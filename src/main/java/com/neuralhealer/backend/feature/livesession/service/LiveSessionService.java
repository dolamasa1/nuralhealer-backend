package com.neuralhealer.backend.feature.livesession.service;

import com.neuralhealer.backend.feature.livesession.dto.LiveSessionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory live session manager.
 * No database — sessions live only while the server is running.
 * Replace with a persistent store when ready for production.
 */
@Service
public class LiveSessionService {

    @Value("${livesession.jitsi-domain:meet.jit.si}")
    private String jitsiDomain;

    @Value("${livesession.room-prefix:neuralhealer-}")
    private String roomPrefix;

    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public LiveSessionResponse create(String creatorName) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        String roomName = roomPrefix + sessionId;

        SessionData data = new SessionData(roomName, creatorName, Instant.now());
        data.participants.add(creatorName);
        sessions.put(sessionId, data);

        return toResponse(sessionId, data);
    }

    public LiveSessionResponse join(String sessionId, String participantName) {
        SessionData data = sessions.get(sessionId);
        if (data == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        if (!data.participants.contains(participantName)) {
            data.participants.add(participantName);
        }
        return toResponse(sessionId, data);
    }

    public LiveSessionResponse get(String sessionId) {
        SessionData data = sessions.get(sessionId);
        if (data == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        return toResponse(sessionId, data);
    }

    public void end(String sessionId) {
        sessions.remove(sessionId);
    }

    // ── internal ──

    private LiveSessionResponse toResponse(String sessionId, SessionData data) {
        return LiveSessionResponse.builder()
                .sessionId(sessionId)
                .roomName(data.roomName)
                .jitsiDomain(jitsiDomain)
                .status("active")
                .createdBy(data.createdBy)
                .participants(List.copyOf(data.participants))
                .createdAt(data.createdAt)
                .build();
    }

    private static class SessionData {
        final String roomName;
        final String createdBy;
        final Instant createdAt;
        final CopyOnWriteArrayList<String> participants = new CopyOnWriteArrayList<>();

        SessionData(String roomName, String createdBy, Instant createdAt) {
            this.roomName = roomName;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }
    }
}
