package com.neuralhealer.backend.controller;

import com.neuralhealer.backend.model.dto.*;
import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.service.EngagementMessageService;
import com.neuralhealer.backend.service.EngagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/engagements")
@RequiredArgsConstructor
@Tag(name = "Engagement", description = "Engagement management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class EngagementController {

    private final EngagementService engagementService;
    private final EngagementMessageService messageService;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate a new engagement (Doctor or Patient)")
    public ResponseEntity<StartEngagementResponse> initiate(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody StartEngagementRequest request) {
        return ResponseEntity.ok(engagementService.initiateEngagement(user, request));
    }

    @PostMapping("/verify-start")
    @Operation(summary = "Verify engagement start with token (Doctor or Patient)")
    public ResponseEntity<EngagementResponse> verifyStart(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VerifyEngagementRequest request) {
        return ResponseEntity.ok(engagementService.verifyStart(user, request.token()));
    }

    @GetMapping("/my-engagements")
    @Operation(summary = "Get all engagements for current user")
    public ResponseEntity<List<EngagementResponse>> getMyEngagements(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(engagementService.getMyEngagements(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get engagement details")
    public ResponseEntity<EngagementResponse> getEngagement(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(engagementService.getEngagement(user, id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hard delete a PENDING engagement (Involved parties only)")
    public ResponseEntity<Void> deleteEngagement(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        engagementService.hardDeleteEngagement(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Soft cancel an engagement (Doctor or Patient)")
    public ResponseEntity<EngagementResponse> cancelEngagement(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody CancelEngagementRequest request) {
        return ResponseEntity.ok(engagementService.cancelEngagement(user, id, request));
    }

    @PostMapping("/{id}/refresh-token")
    @Operation(summary = "Refresh START token for a pending engagement (Initiator only)")
    public ResponseEntity<TokenResponse> refreshToken(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(engagementService.refreshToken(user, id));
    }

    @GetMapping("/{id}/token")
    @Operation(summary = "Get current valid START token (Initiator only)")
    public ResponseEntity<TokenResponse> getCurrentToken(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(engagementService.getCurrentToken(user, id));
    }

    @PostMapping("/{id}/end-request")
    @Operation(summary = "Request to end an engagement")
    public ResponseEntity<StartEngagementResponse> requestEnd(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody EndEngagementRequest request) {
        return ResponseEntity.ok(engagementService.requestEnd(user, id, request.reason()));
    }

    @PostMapping("/{id}/verify-end")
    @Operation(summary = "Verify engagement end with token")
    public ResponseEntity<EngagementResponse> verifyEnd(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody VerifyEngagementRequest request) {
        return ResponseEntity.ok(engagementService.verifyEnd(user, request.token()));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Send a message in an engagement")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(messageService.sendMessage(user, id, request));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get messages for an engagement")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(messageService.getMessages(user, id));
    }
}
