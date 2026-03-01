package com.neuralhealer.backend.shared.websocket;

import com.neuralhealer.backend.shared.websocket.NotificationWebSocketHandler;
import com.neuralhealer.backend.feature.livesession.websocket.WebRtcSignalingSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class RawWebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationHandler;
    private final WebRtcSignalingSocketHandler webRtcHandler;

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        // Register notifications
        registry.addHandler(notificationHandler, "/notifications")
                .setAllowedOriginPatterns("*"); // Allow Postman/Cross-origin handshake

        // Register WebRTC signaling
        registry.addHandler(webRtcHandler, "/ws/webrtc")
                .setAllowedOriginPatterns("*");
    }
}
