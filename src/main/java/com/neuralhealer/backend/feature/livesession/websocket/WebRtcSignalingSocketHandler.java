package com.neuralhealer.backend.feature.livesession.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebRTC Signaling Server.
 * Routes SPD offers, answers, and ICE candidates between peers in the same
 * room.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebRtcSignalingSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    // Map: RoomId (SessionId) -> List of connected WebSocketSessions
    private final Map<String, CopyOnWriteArrayList<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // Map: WebSocketSession ID -> RoomId (to know which room to broadcast to on
    // disconnect)
    private final Map<String, String> sessionToRoomMap = new ConcurrentHashMap<>();

    // Map: WebSocketSession ID -> PeerId (DisplayName)
    private final Map<String, String> sessionToPeerIdMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        log.info("WebRTC signaling connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        Map<String, Object> payload;
        try {
            // Suppress unchecked cast warning since we know it's a JSON object
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(message.getPayload(), Map.class);
            payload = parsed;
        } catch (Exception e) {
            log.error("Failed to parse WebRTC signaling message", e);
            return;
        }

        String type = (String) payload.get("type");
        String roomId = (String) payload.get("roomId");

        if (roomId == null)
            return;

        String peerId = (String) payload.get("peerId");
        if (peerId != null) {
            sessionToPeerIdMap.put(session.getId(), peerId);
        }

        switch (type) {
            case "join":
                handleJoin(session, roomId, payload);
                break;
            case "status-update":
            case "offer":
            case "answer":
            case "ice-candidate":
            case "leave":
            case "join-request":
                // Guest requests to join: register their session so they can receive the
                // approval/denial
                roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(session);
                sessionToRoomMap.put(session.getId(), roomId);
                log.info("Client {} requesting to join WebRTC room {}", session.getId(), roomId);
                broadcastToOthers(session, roomId, message);
                break;
            case "join-approved":
            case "join-denied":
                broadcastToOthers(session, roomId, message);
                break;
            default:
                log.warn("Unknown WebRTC signal type: {}", type);
        }
    }

    private void handleJoin(WebSocketSession session, String roomId, Map<String, Object> payload) throws IOException {
        roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(session);
        sessionToRoomMap.put(session.getId(), roomId);

        log.info("Client {} joined WebRTC room {}", session.getId(), roomId);

        // Notify others in the room that someone joined
        broadcastToOthers(session, roomId, new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void broadcastToOthers(WebSocketSession senderSession, String roomId, TextMessage message) {
        CopyOnWriteArrayList<WebSocketSession> peers = roomSessions.get(roomId);
        if (peers == null)
            return;

        for (WebSocketSession peer : peers) {
            if (peer.isOpen() && !peer.getId().equals(senderSession.getId())) {
                try {
                    peer.sendMessage(message);
                } catch (IOException e) {
                    log.error("Failed to send WebRTC signal to peer {}", peer.getId(), e);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String roomId = sessionToRoomMap.remove(session.getId());
        if (roomId != null) {
            CopyOnWriteArrayList<WebSocketSession> peers = roomSessions.get(roomId);
            if (peers != null) {
                peers.remove(session);

                // Tell remaining peers someone left, using their actual peerId
                String peerId = sessionToPeerIdMap.remove(session.getId());
                if (peerId == null) {
                    peerId = session.getId(); // Fallback
                }

                try {
                    String leaveMsg = objectMapper.writeValueAsString(Map.of(
                            "type", "leave",
                            "roomId", roomId,
                            "peerId", peerId));
                    broadcastToOthers(session, roomId, new TextMessage(leaveMsg));
                } catch (Exception e) {
                    log.error("Failed to broadcast leave message", e);
                }

                if (peers.isEmpty()) {
                    roomSessions.remove(roomId);
                }
            }
            log.info("Client {} left WebRTC room {}", session.getId(), roomId);
        }
    }
}
