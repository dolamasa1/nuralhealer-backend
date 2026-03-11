package com.neuralhealer.backend.feature.livesession.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

/**
 * WebRTC Signaling Server.
 * Routes SDP offers, answers, and ICE candidates between peers in the same room.
 *
 * Production hardening:
 * - Max peers per room (default 10)
 * - Total connection limit (default 500)
 * - Message size cap (64 KB)
 * - Stale room cleanup every 60 s
 * - Dead session eviction on broadcast failure
 */
@Slf4j
@Component
public class WebRtcSignalingSocketHandler extends TextWebSocketHandler {

    private static final int MAX_PEERS_PER_ROOM = 10;
    private static final int MAX_TOTAL_CONNECTIONS = 500;
    private static final int MAX_MESSAGE_SIZE = 64 * 1024; // 64 KB
    private static final long ROOM_TTL_SECONDS = 2 * 60 * 60; // 2 hours

    private final ObjectMapper objectMapper;

    // Room ID -> list of sessions in that room
    private final Map<String, CopyOnWriteArrayList<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // WebSocket session ID -> room ID
    private final Map<String, String> sessionToRoomMap = new ConcurrentHashMap<>();

    // WebSocket session ID -> peer display-name
    private final Map<String, String> sessionToPeerIdMap = new ConcurrentHashMap<>();

    // Room ID -> last activity timestamp (for TTL cleanup — updated on every message)
    private final Map<String, Instant> roomLastActivity = new ConcurrentHashMap<>();

    private ScheduledExecutorService cleanupExecutor;

    public WebRtcSignalingSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /* ─── Lifecycle ─── */

    @PostConstruct
    void startCleanup() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "webrtc-room-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(this::evictStaleRooms, 60, 60, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stopCleanup() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdownNow();
        }
    }

    /* ─── WebSocket callbacks ─── */

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        if (sessionToRoomMap.size() >= MAX_TOTAL_CONNECTIONS) {
            log.warn("WebRTC connection rejected – total limit reached ({})", MAX_TOTAL_CONNECTIONS);
            try { session.close(CloseStatus.SERVICE_OVERLOAD); } catch (IOException ignored) {}
            return;
        }
        log.info("WebRTC signaling connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        // Reject oversized messages
        if (message.getPayloadLength() > MAX_MESSAGE_SIZE) {
            log.warn("Dropping oversized WebRTC message ({} bytes) from {}", message.getPayloadLength(), session.getId());
            return;
        }

        Map<String, Object> payload;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(message.getPayload(), Map.class);
            payload = parsed;
        } catch (Exception e) {
            log.error("Failed to parse WebRTC signaling message", e);
            return;
        }

        String type = (String) payload.get("type");
        String roomId = (String) payload.get("roomId");
        if (type == null || roomId == null) return;

        // Sanitize roomId length
        if (roomId.length() > 64) {
            log.warn("Ignoring message with oversized roomId from {}", session.getId());
            return;
        }

        String peerId = (String) payload.get("peerId");
        if (peerId != null) {
            sessionToPeerIdMap.put(session.getId(), peerId);
        }

        switch (type) {
            case "join":
                handleJoin(session, roomId, payload);
                break;
            case "join-request":
                handleJoinRequest(session, roomId, message);
                break;
            case "status-update":
            case "offer":
            case "answer":
            case "ice-candidate":
            case "leave":
                broadcastToOthers(session, roomId, message);
                break;
            case "join-approved":
            case "join-denied":
                broadcastToOthers(session, roomId, message);
                break;
            case "ping":
                // Keepalive from client — touch activity timestamp, no broadcast needed
                roomLastActivity.put(roomId, Instant.now());
                break;
            default:
                log.warn("Unknown WebRTC signal type: {}", type);
        }
    }

    /* ─── Handlers ─── */

    private void handleJoin(WebSocketSession session, String roomId, Map<String, Object> payload) throws IOException {
        CopyOnWriteArrayList<WebSocketSession> peers = roomSessions.computeIfAbsent(roomId, k -> {
            roomLastActivity.put(k, Instant.now());
            return new CopyOnWriteArrayList<>();
        });

        if (peers.size() >= MAX_PEERS_PER_ROOM) {
            log.warn("Room {} full – rejecting {}", roomId, session.getId());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    Map.of("type", "error", "message", "Room is full"))));
            return;
        }

        // Avoid duplicate registration
        if (!containsSession(peers, session)) {
            peers.add(session);
        }
        sessionToRoomMap.put(session.getId(), roomId);

        log.info("Client {} joined WebRTC room {} (peers: {})", session.getId(), roomId, peers.size());

        broadcastToOthers(session, roomId, new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void handleJoinRequest(WebSocketSession session, String roomId, TextMessage message) {
        CopyOnWriteArrayList<WebSocketSession> peers = roomSessions.computeIfAbsent(roomId, k -> {
            roomLastActivity.put(k, Instant.now());
            return new CopyOnWriteArrayList<>();
        });

        // Register the guest session only once so they can receive approval/denial
        if (!containsSession(peers, session)) {
            peers.add(session);
        }
        sessionToRoomMap.put(session.getId(), roomId);
        log.info("Client {} requesting to join WebRTC room {}", session.getId(), roomId);
        broadcastToOthers(session, roomId, message);
    }

    /* ─── Broadcast ─── */

    private void broadcastToOthers(WebSocketSession sender, String roomId, TextMessage message) {
        CopyOnWriteArrayList<WebSocketSession> peers = roomSessions.get(roomId);
        if (peers == null) return;

        // Touch room activity so active rooms are never evicted
        roomLastActivity.put(roomId, Instant.now());

        Iterator<WebSocketSession> it = peers.iterator();
        while (it.hasNext()) {
            WebSocketSession peer = it.next();
            if (!peer.isOpen()) {
                peers.remove(peer);
                sessionToRoomMap.remove(peer.getId());
                sessionToPeerIdMap.remove(peer.getId());
                continue;
            }
            if (!peer.getId().equals(sender.getId())) {
                try {
                    peer.sendMessage(message);
                } catch (IOException e) {
                    log.error("Failed to send WebRTC signal to peer {} – removing", peer.getId(), e);
                    peers.remove(peer);
                    sessionToRoomMap.remove(peer.getId());
                    sessionToPeerIdMap.remove(peer.getId());
                }
            }
        }
    }

    /* ─── Disconnect ─── */

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String roomId = sessionToRoomMap.remove(session.getId());
        if (roomId != null) {
            CopyOnWriteArrayList<WebSocketSession> peers = roomSessions.get(roomId);
            if (peers != null) {
                peers.remove(session);

                String peerId = sessionToPeerIdMap.remove(session.getId());
                if (peerId == null) {
                    peerId = session.getId();
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
                    roomLastActivity.remove(roomId);
                }
            }
            log.info("Client {} left WebRTC room {}", session.getId(), roomId);
        } else {
            sessionToPeerIdMap.remove(session.getId());
        }
    }

    /* ─── Cleanup ─── */

    private void evictStaleRooms() {
        Instant cutoff = Instant.now().minusSeconds(ROOM_TTL_SECONDS);
        // Snapshot the keys so we don't hold the entrySet iterator across mutations
        for (String roomId : new java.util.ArrayList<>(roomLastActivity.keySet())) {
            Instant last = roomLastActivity.get(roomId);
            if (last == null || !last.isBefore(cutoff)) continue;
            // Atomic CAS: only remove if activity hasn't been updated since our read;
            // if a live message arrived between get() and here, remove(key, value) returns false
            if (!roomLastActivity.remove(roomId, last)) continue;
            CopyOnWriteArrayList<WebSocketSession> peers = roomSessions.remove(roomId);
            if (peers != null) {
                for (WebSocketSession s : peers) {
                    sessionToRoomMap.remove(s.getId());
                    sessionToPeerIdMap.remove(s.getId());
                    try { s.close(CloseStatus.GOING_AWAY); } catch (IOException ignored) {}
                }
                log.info("Evicted stale WebRTC room {} ({} peers)", roomId, peers.size());
            }
        }
    }

    private boolean containsSession(CopyOnWriteArrayList<WebSocketSession> peers, WebSocketSession session) {
        for (WebSocketSession s : peers) {
            if (s.getId().equals(session.getId())) return true;
        }
        return false;
    }
}
