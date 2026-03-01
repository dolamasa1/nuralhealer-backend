package com.neuralhealer.backend.feature.livesession.provider;

import com.neuralhealer.backend.feature.livesession.dto.LiveSessionResponse;

public interface LiveSessionProvider {
    /**
     * Identifies which provider implementation this is (e.g. jitsi, native-webrtc).
     */
    String getProviderName();

    LiveSessionResponse create(String sessionId, String roomName, String creatorName);

    LiveSessionResponse join(String sessionId, String roomName, String participantName,
            java.util.List<String> currentParticipants);
}
