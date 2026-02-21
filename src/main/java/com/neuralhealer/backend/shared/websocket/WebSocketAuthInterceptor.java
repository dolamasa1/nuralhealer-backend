package com.neuralhealer.backend.shared.websocket;

import com.neuralhealer.backend.shared.websocket.WebSocketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final WebSocketService webSocketService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 1. Check if user is already authenticated
            if (accessor.getUser() != null) {
                return message;
            }

            String token = extractToken(accessor);

            // 2. Try JWT Authentication via centralized service
            if (token != null) {
                webSocketService.validateToken(token).ifPresent(auth -> {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    accessor.setUser(auth);
                    log.debug("WebSocket connection authenticated for user: {}", auth.getName());
                });
            }

            // 3. Fallback: Allow anonymous guest access
            if (accessor.getUser() == null) {
                UsernamePasswordAuthenticationToken guestAuth = webSocketService
                        .createGuestAuthentication(accessor.getSessionId());
                SecurityContextHolder.getContext().setAuthentication(guestAuth);
                accessor.setUser(guestAuth);
                log.debug("WebSocket connection authenticated as Anonymous Guest: {}", guestAuth.getName());
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Priority 1: Authorization Header
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            String authHeader = authorization.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        // Priority 2: Cookie Header
        List<String> cookies = accessor.getNativeHeader("Cookie");
        if (cookies != null && !cookies.isEmpty()) {
            String cookieHeader = cookies.get(0);
            String[] cookiePairs = cookieHeader.split(";");
            for (String cookie : cookiePairs) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && "neuralhealer_token".equals(parts[0])) {
                    return parts[1];
                }
            }
        }

        return null;
    }
}
