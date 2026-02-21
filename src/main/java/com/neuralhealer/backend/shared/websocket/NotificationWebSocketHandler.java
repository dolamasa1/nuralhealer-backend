package com.neuralhealer.backend.shared.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuralhealer.backend.shared.websocket.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Raw WebSocket Handler for future push notifications, reminders, and alerts.
 * Dedicated pathway for server-to-client broadcasts.
 */
@Component
@Slf4j
public class NotificationWebSocketHandler extends BaseWebSocketHandler {

    public NotificationWebSocketHandler(WebSocketService webSocketService, ObjectMapper objectMapper) {
        super(webSocketService, objectMapper);
    }

    @Override
    protected String getHandlerName() {
        return "Notifications";
    }

    @Override
    protected boolean allowAnonymous() {
        // Notifications are sensitive, only authenticated users allowed
        return false;
    }

    @Override
    protected void onConnectionStarted(WebSocketSession session, UsernamePasswordAuthenticationToken auth) {
        // Here we could send the last N unread notifications if needed
        log.debug("Notification stream started for: {}", auth.getName());
    }
}
