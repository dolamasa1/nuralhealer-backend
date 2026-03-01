package com.neuralhealer.backend.feature.livesession.provider.jitsi;

import com.neuralhealer.backend.feature.livesession.dto.LiveSessionResponse;
import com.neuralhealer.backend.feature.livesession.provider.LiveSessionProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JitsiProviderImpl implements LiveSessionProvider {

    @Value("${livesession.jitsi-domain:meet.ffmuc.net}")
    private String jitsiDomain;

    @Value("${livesession.jwt-secret:neuralhealer-jitsi-secret-key-32chars}")
    private String jwtSecret;

    @Value("${livesession.jwt-app-id:neuralhealer}")
    private String jwtAppId;

    @Override
    public String getProviderName() {
        return "jitsi";
    }

    @Override
    public LiveSessionResponse create(String sessionId, String roomName, String creatorName) {
        return buildResponse(sessionId, roomName, creatorName, List.of(creatorName), true);
    }

    @Override
    public LiveSessionResponse join(String sessionId, String roomName, String participantName,
            List<String> currentParticipants) {
        return buildResponse(sessionId, roomName, participantName, currentParticipants, true);
    }

    private LiveSessionResponse buildResponse(String sessionId, String roomName, String displayName,
            List<String> participants, boolean isModerator) {
        String jwt = (displayName != null) ? buildJwt(roomName, displayName, isModerator) : null;
        return LiveSessionResponse.builder()
                .sessionId(sessionId)
                .roomName(roomName)
                .provider(getProviderName())
                .jitsiDomain(jitsiDomain)
                .jitsiJwt(jwt)
                .status("active")
                .createdBy(participants.isEmpty() ? displayName : participants.get(0))
                .participants(participants)
                .createdAt(Instant.now())
                .build();
    }

    private String buildJwt(String roomName, String displayName, boolean isModerator) {
        try {
            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                keyBytes = java.util.Arrays.copyOf(keyBytes, 32);
            }
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);

            Map<String, Object> context = new HashMap<>();
            context.put("user", Map.of(
                    "name", displayName != null ? displayName : "Guest",
                    "moderator", isModerator));
            context.put("features", Map.of(
                    "livestreaming", false,
                    "outbound-call", false,
                    "transcription", false,
                    "recording", false));

            return Jwts.builder()
                    .header().add("kid", jwtAppId).and()
                    .issuer(jwtAppId)
                    .subject(jitsiDomain)
                    .audience().add(jitsiDomain).and()
                    .claim("room", roomName.toLowerCase())
                    .claim("context", context)
                    .issuedAt(new Date())
                    .expiration(Date.from(Instant.now().plusSeconds(3600)))
                    .signWith(key)
                    .compact();
        } catch (Exception e) {
            return null;
        }
    }
}
