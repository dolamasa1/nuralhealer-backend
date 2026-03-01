package com.neuralhealer.backend.feature.livesession.provider.nativewebrtc;

import com.neuralhealer.backend.feature.livesession.dto.LiveSessionResponse;
import com.neuralhealer.backend.feature.livesession.provider.LiveSessionProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class NativeWebRtcProviderImpl implements LiveSessionProvider {

    @Override
    public String getProviderName() {
        return "native-webrtc";
    }

    @Override
    public LiveSessionResponse create(String sessionId, String roomName, String creatorName) {
        return buildResponse(sessionId, roomName, creatorName, List.of(creatorName));
    }

    @Override
    public LiveSessionResponse join(String sessionId, String roomName, String participantName,
            List<String> currentParticipants) {
        return buildResponse(sessionId, roomName, participantName, currentParticipants);
    }

    private LiveSessionResponse buildResponse(String sessionId, String roomName, String displayName,
            List<String> participants) {
        return LiveSessionResponse.builder()
                .sessionId(sessionId)
                .roomName(roomName)
                .provider(getProviderName())
                // We do not need Jitsi domain or JWT for native WebRTC
                .jitsiDomain(null)
                .jitsiJwt(null)
                .status("active")
                .createdBy(participants.isEmpty() ? displayName : participants.get(0))
                .participants(participants)
                .createdAt(Instant.now())
                .build();
    }
}
