package com.neuralhealer.backend.controller;

import com.neuralhealer.backend.model.entity.AiChatMessage;
import com.neuralhealer.backend.model.entity.AiChatSession;
import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.service.ChatStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Tag(name = "AI Chat History", description = "Endpoints for patients to view their AI chat history")
public class ChatHistoryController {

    private final ChatStorageService chatStorageService;

    @GetMapping
    @Operation(summary = "Get my sessions", description = "Retrieve all chat sessions for the authenticated user")
    public List<AiChatSession> getMySessions(@AuthenticationPrincipal User user) {
        return chatStorageService.getUserSessions(user.getId());
    }

    @GetMapping("/search")
    @Operation(summary = "Search chats", description = "Search sessions by title or message content")
    public List<AiChatSession> searchChats(
            @RequestParam String q,
            @AuthenticationPrincipal User user) {
        return chatStorageService.searchSessions(user.getId(), q);
    }

    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "Get session messages", description = "Retrieve all messages for a specific session")
    public List<AiChatMessage> getMessages(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User user) {
        // Verify session belongs to user (Security Check)
        // For efficiency, we could check this in the repository, but for now we'll
        // filter or rely on ID match
        // A robust check:
        List<AiChatSession> sessions = chatStorageService.getUserSessions(user.getId());
        boolean isOwner = sessions.stream().anyMatch(s -> s.getId().equals(sessionId));

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this session");
        }

        return chatStorageService.getSessionMessages(sessionId);
    }

    @PutMapping("/{sessionId}/title")
    @Operation(summary = "Update session title", description = "Rename a chat session")
    public void updateTitle(
            @PathVariable UUID sessionId,
            @RequestBody String title,
            @AuthenticationPrincipal User user) {
        // Verify ownership
        // Optimization: Create specific existsBy method in repo later if needed.
        // For now, reusing the list check is acceptable for N < 100 sessions
        List<AiChatSession> sessions = chatStorageService.getUserSessions(user.getId());
        boolean isOwner = sessions.stream().anyMatch(s -> s.getId().equals(sessionId));

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this session");
        }

        chatStorageService.updateSessionTitle(sessionId, title);
    }
}
