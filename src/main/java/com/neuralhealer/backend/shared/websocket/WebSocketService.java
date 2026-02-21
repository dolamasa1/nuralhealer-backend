package com.neuralhealer.backend.shared.websocket;

import com.neuralhealer.backend.shared.entity.User;
import com.neuralhealer.backend.feature.auth.repository.UserRepository;
import com.neuralhealer.backend.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to handle shared logic between different WebSocket protocols.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    // Unified Session Registry: Map<SessionId, Protocol_UserIdentifier>
    private final Map<String, String> sessionRegistry = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Validates a JWT token and returns an Authentication object if valid.
     */
    public Optional<UsernamePasswordAuthenticationToken> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            String email = jwtService.extractUsername(token);
            if (email != null) {
                User user = userRepository.findByEmail(email).orElse(null);

                if (user != null && jwtService.isTokenValid(token, user)) {
                    log.debug("WebSocket token validated for user: {}", email);
                    return Optional.of(new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities()));
                }
            }
        } catch (Exception e) {
            log.error("WebSocket token validation error: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Creates a guest authentication for unauthenticated (but allowed) connections.
     */
    public UsernamePasswordAuthenticationToken createGuestAuthentication(String sessionId) {
        String guestId = "guest_" + sessionId;
        log.debug("Creating guest authentication for session: {}", sessionId);
        return new UsernamePasswordAuthenticationToken(
                guestId,
                null,
                List.of());
    }

    /**
     * Registers a session in the unified registry.
     */
    public void registerSession(String sessionId, String protocol, String userIdentifier) {
        String info = protocol + ":" + userIdentifier;
        sessionRegistry.put(sessionId, info);
        log.debug("Session Registered: {} -> {} (Total: {})", sessionId, info, sessionRegistry.size());
    }

    /**
     * Removes a session from the unified registry.
     */
    public void removeSession(String sessionId) {
        String info = sessionRegistry.remove(sessionId);
        if (info != null) {
            log.debug("Session Removed: {} (Total: {})", info, sessionRegistry.size());
        }
    }

    /**
     * Gets a snapshot of the current registry.
     */
    public Map<String, String> getActiveSessions() {
        return Map.copyOf(sessionRegistry);
    }
}
