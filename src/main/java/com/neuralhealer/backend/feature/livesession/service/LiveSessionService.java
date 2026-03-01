package com.neuralhealer.backend.feature.livesession.service;

import com.neuralhealer.backend.feature.livesession.dto.LiveSessionResponse;
import com.neuralhealer.backend.feature.livesession.provider.LiveSessionProvider;
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
 * Delegates actual session generation to the active LiveSessionProvider.
 */
@Service
public class LiveSessionService {

    @Value("${livesession.provider:native-webrtc}")
    private String activeProviderName;

    @Value("${livesession.room-prefix:neuralhealer-}")
    private String roomPrefix;

    private final Map<String, LiveSessionProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public LiveSessionService(List<LiveSessionProvider> providerList) {
        for (LiveSessionProvider p : providerList) {
            this.providers.put(p.getProviderName(), p);
        }
    }

    private LiveSessionProvider getActiveProvider() {
        LiveSessionProvider p = providers.get(activeProviderName);
        if (p == null) {
            throw new IllegalStateException("Unknown livesession provider configured: " + activeProviderName);
        }
        return p;
    }

    public LiveSessionResponse create(String creatorName) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        String roomName = roomPrefix + sessionId;

        SessionData data = new SessionData(roomName, creatorName, Instant.now());
        data.participants.add(creatorName);
        sessions.put(sessionId, data);

        return getActiveProvider().create(sessionId, roomName, creatorName);
    }

    public LiveSessionResponse join(String sessionId, String participantName) {
        SessionData data = sessions.get(sessionId);
        if (data == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        if (!data.participants.contains(participantName)) {
            data.participants.add(participantName);
        }

        return getActiveProvider().join(sessionId, data.roomName, participantName, List.copyOf(data.participants));
    }

    public LiveSessionResponse get(String sessionId) {
        SessionData data = sessions.get(sessionId);
        if (data == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        // Treat as a join without a new participant name to just get the current state
        return getActiveProvider().join(sessionId, data.roomName, null, List.copyOf(data.participants));
    }

    public void end(String sessionId) {
        sessions.remove(sessionId);
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
