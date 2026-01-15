package com.neuralhealer.backend.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuralhealer.backend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared base for all raw WebSocket handlers.
 * Provides security, session management, and JSON mapping.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseWebSocketHandler extends TextWebSocketHandler {

    protected final WebSocketService webSocketService;
    protected final ObjectMapper objectMapper;

    // Active sessions for this specific handler
    protected final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        // Authenticate the session
        String token = extractToken(session);
        Optional<UsernamePasswordAuthenticationToken> auth = webSocketService.validateToken(token);

        if (auth.isEmpty()) {
            // Check if anonymous access is allowed for this specific handler
            if (!allowAnonymous()) {
                log.warn("Raw WebSocket connection rejected (Unauthenticated): {}", session.getId());
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            // Assign guest authentication
            auth = Optional.of(webSocketService.createGuestAuthentication(session.getId()));
        }

        // Store authentication in session attributes for easy access
        session.getAttributes().put("auth", auth.get());
        sessions.put(session.getId(), session);

        // Register in unified registry
        webSocketService.registerSession(session.getId(), "RAW-" + getHandlerName(), auth.get().getName());

        log.info("WebSocket[{}] connected: {} (User: {})",
                getHandlerName(), session.getId(), auth.get().getName());

        onConnectionStarted(session, auth.get());
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        webSocketService.removeSession(session.getId());
        log.info("WebSocket[{}] disconnected: {} (Status: {})",
                getHandlerName(), session.getId(), status);
    }

    protected void sendJson(WebSocketSession session, Object data) throws IOException {
        if (session != null && session.isOpen()) {
            String json = objectMapper.writeValueAsString(data);
            if (json != null) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }

    private String extractToken(WebSocketSession session) {
        // Extract from headers or query params if necessary
        // For now, looking at 'Authorization' header in handshake headers
        String authHeader = session.getHandshakeHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // Methods to be implemented by child classes
    protected abstract String getHandlerName();

    protected abstract boolean allowAnonymous();

    protected void onConnectionStarted(WebSocketSession session, UsernamePasswordAuthenticationToken auth) {
    }
}
