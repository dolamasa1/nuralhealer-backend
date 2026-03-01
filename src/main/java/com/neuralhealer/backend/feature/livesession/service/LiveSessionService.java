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

    private LiveSessionProvider getProvider(String providerName) {
        String name = (providerName != null && !providerName.isEmpty()) ? providerName : activeProviderName;
        LiveSessionProvider p = providers.get(name);
        if (p == null) {
            // Fallback to default if specified provider not found
            p = providers.get(activeProviderName);
        }
        if (p == null) {
            throw new IllegalStateException("No livesession provider available.");
        }
        return p;
    }

    public LiveSessionResponse create(String creatorName, String providerName) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        String roomName = roomPrefix + sessionId;

        LiveSessionProvider provider = getProvider(providerName);
        SessionData data = new SessionData(roomName, creatorName, Instant.now(), provider.getProviderName());
        data.participants.add(creatorName);
        sessions.put(sessionId, data);

        return provider.create(sessionId, roomName, creatorName);
    }

    public LiveSessionResponse join(String sessionId, String participantName, String providerName) {
        SessionData data = sessions.get(sessionId);
        if (data == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        if (participantName != null && !data.participants.contains(participantName)) {
            data.participants.add(participantName);
        }

        // Use the provider from the session if it exists, otherwise the one from
        // request, otherwise default
        String pName = (data.provider != null) ? data.provider : providerName;
        return getProvider(pName).join(sessionId, data.roomName, participantName, List.copyOf(data.participants));
    }

    public LiveSessionResponse get(String sessionId) {
        return join(sessionId, null, null);
    }

    public void end(String sessionId) {
        sessions.remove(sessionId);
    }

    private static class SessionData {
        final String roomName;
        final String createdBy;
        final Instant createdAt;
        final String provider;
        final CopyOnWriteArrayList<String> participants = new CopyOnWriteArrayList<>();

        SessionData(String roomName, String createdBy, Instant createdAt, String provider) {
            this.roomName = roomName;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
            this.provider = provider;
        }
    }
}
