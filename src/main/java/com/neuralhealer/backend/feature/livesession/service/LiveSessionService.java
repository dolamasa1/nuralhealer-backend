package com.neuralhealer.backend.feature.livesession.service;

import com.neuralhealer.backend.feature.livesession.dto.LiveSessionResponse;
import com.neuralhealer.backend.feature.livesession.provider.LiveSessionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory live session manager.
 * Delegates actual session generation to the active LiveSessionProvider.
 *
 * Production hardening:
 * - Max participants per session (default 10)
 * - Stale session cleanup every 5 minutes (TTL: 2 hours)
 */
@Slf4j
@Service
public class LiveSessionService {

    private static final int MAX_PARTICIPANTS = 10;
    private static final long SESSION_TTL_SECONDS = 2 * 60 * 60; // 2 hours

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

        log.info("Live session created: {} by {} (total active: {})", sessionId, creatorName, sessions.size());
        return provider.create(sessionId, roomName, creatorName);
    }

    public LiveSessionResponse join(String sessionId, String participantName, String providerName) {
        SessionData data = sessions.get(sessionId);
        if (data == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        if (participantName != null && !data.participants.contains(participantName)) {
            if (data.participants.size() >= MAX_PARTICIPANTS) {
                throw new IllegalStateException("Session is full (max " + MAX_PARTICIPANTS + " participants)");
            }
            data.participants.add(participantName);
        }

        // Touch the session so it doesn't expire while active
        data.lastActivity = Instant.now();

        String pName = (data.provider != null) ? data.provider : providerName;
        return getProvider(pName).join(sessionId, data.roomName, participantName, List.copyOf(data.participants));
    }

    public LiveSessionResponse get(String sessionId) {
        return join(sessionId, null, null);
    }

    public void end(String sessionId) {
        SessionData removed = sessions.remove(sessionId);
        if (removed != null) {
            log.info("Live session ended: {} (active: {})", sessionId, sessions.size());
        }
    }

    /** Remove sessions older than TTL that have had no activity. */
    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    void evictStaleSessions() {
        Instant cutoff = Instant.now().minusSeconds(SESSION_TTL_SECONDS);
        Iterator<Map.Entry<String, SessionData>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SessionData> entry = it.next();
            if (entry.getValue().lastActivity.isBefore(cutoff)) {
                it.remove();
                log.info("Evicted stale live session: {} (created by {})", entry.getKey(), entry.getValue().createdBy);
            }
        }
    }

    private static class SessionData {
        final String roomName;
        final String createdBy;
        final Instant createdAt;
        final String provider;
        final CopyOnWriteArrayList<String> participants = new CopyOnWriteArrayList<>();
        volatile Instant lastActivity;

        SessionData(String roomName, String createdBy, Instant createdAt, String provider) {
            this.roomName = roomName;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
            this.provider = provider;
            this.lastActivity = createdAt;
        }
    }
}
