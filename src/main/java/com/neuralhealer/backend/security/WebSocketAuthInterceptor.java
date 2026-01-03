package com.neuralhealer.backend.security;

import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.repository.UserRepository;

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

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            // JwtService.isTokenValid requires UserDetails, but we don't have it yet.
            // We'll extract username first, then load user, then validate.
            if (token != null) {
                try {
                    String email = jwtService.extractUsername(token);
                    if (email != null) {
                        User user = userRepository.findByEmail(email).orElse(null);

                        if (user != null && jwtService.isTokenValid(token, user)) {
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user,
                                    null, List.of()); // authorities can be empty or from user
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            accessor.setUser(auth);

                            log.info("WebSocket connection authenticated for user: {}", email);
                        } else {
                            log.warn("WebSocket authentication failed: Invalid token or user not found for email {}",
                                    email);
                        }
                    }
                } catch (Exception e) {
                    log.warn("WebSocket authentication error: {}", e.getMessage());
                }
            } else {
                log.warn("WebSocket authentication failed: Missing token");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Priority 1: Authorization Header (Bearer <token>)
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            String authHeader = authorization.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        // Priority 2: Cookie Header (neuralhealer_token=<token>)
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
