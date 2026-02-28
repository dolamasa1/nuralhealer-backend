package com.neuralhealer.backend.feature.livesession.controller;

import com.neuralhealer.backend.feature.livesession.dto.CreateSessionRequest;
import com.neuralhealer.backend.feature.livesession.dto.JoinSessionRequest;
import com.neuralhealer.backend.feature.livesession.dto.LiveSessionResponse;
import com.neuralhealer.backend.feature.livesession.service.LiveSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/live-sessions")
@RequiredArgsConstructor
public class LiveSessionController {

    private final LiveSessionService liveSessionService;

    @PostMapping
    public ResponseEntity<LiveSessionResponse> create(@RequestBody CreateSessionRequest request) {
        LiveSessionResponse session = liveSessionService.create(request.getParticipantName());
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<LiveSessionResponse> get(@PathVariable String sessionId) {
        return ResponseEntity.ok(liveSessionService.get(sessionId));
    }

    @PostMapping("/{sessionId}/join")
    public ResponseEntity<LiveSessionResponse> join(
            @PathVariable String sessionId,
            @RequestBody JoinSessionRequest request) {
        return ResponseEntity.ok(liveSessionService.join(sessionId, request.getParticipantName()));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> end(@PathVariable String sessionId) {
        liveSessionService.end(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session ended"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
