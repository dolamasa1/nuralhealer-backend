package com.neuralhealer.backend.integration.gmail;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for testing email templates.
 */
@RestController
@RequestMapping("/test/email")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email Testing", description = "Test email templates")
public class EmailTestController {

    private final DirectEmailService directEmailService;

    @Data
    public static class VerificationRequest {
        private String email;
        private String code = "123456";
    }

    @Data
    public static class PasswordResetRequest {
        private String email;
        private String token = "test-token-123";
    }

    @Data
    public static class SpecialThanksRequest {
        private String email;
        private String name = "Ahmed";
        private String acknowledgment = "For the one whose vision turns silence into symphony";
        private String message = "Today, Neural Healer's voice learned to speak on its own. Our words now flow without pause, reaching those who need them most, exactly when they need to hear us.";
        private String note = "You hold the very first breath of this new chapter.";
    }

    @Operation(summary = "Send test verification email")
    @PostMapping("/verification")
    public ResponseEntity<Map<String, String>> testVerificationEmail(@RequestBody VerificationRequest request) {
        log.info("Sending verification email to: {}", request.email);
        directEmailService.sendVerificationEmail(request.email, request.code);
        return ResponseEntity.ok(Map.of("message", "Verification email sent to " + request.email));
    }

    @Operation(summary = "Send test password reset email")
    @PostMapping("/password-reset")
    public ResponseEntity<Map<String, String>> testPasswordResetEmail(@RequestBody PasswordResetRequest request) {
        log.info("Sending password reset email to: {}", request.email);
        directEmailService.sendPasswordResetEmail(request.email, request.token);
        return ResponseEntity.ok(Map.of("message", "Password reset email sent to " + request.email));
    }

    @Operation(summary = "Send special thanks email")
    @PostMapping("/special-thanks")
    public ResponseEntity<Map<String, String>> testSpecialThanksEmail(@RequestBody SpecialThanksRequest request) {
        log.info("Sending special thanks email to: {}", request.email);
        directEmailService.sendSpecialThanksEmail(
                request.email,
                request.name,
                request.acknowledgment,
                request.message,
                request.note);
        return ResponseEntity.ok(Map.of("message", "Special thanks email sent to " + request.email));
    }
}
