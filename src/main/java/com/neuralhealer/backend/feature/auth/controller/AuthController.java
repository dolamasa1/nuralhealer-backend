package com.neuralhealer.backend.feature.auth.controller;

import com.neuralhealer.backend.feature.auth.dto.AuthResponse;
import com.neuralhealer.backend.feature.auth.dto.LoginRequest;
import com.neuralhealer.backend.feature.auth.dto.RegisterRequest;
import com.neuralhealer.backend.feature.auth.dto.ResendOtpRequest;
import com.neuralhealer.backend.feature.auth.dto.VerifyEmailRequest;
import com.neuralhealer.backend.feature.auth.service.AuthService;
import com.neuralhealer.backend.feature.auth.service.OtpService;
import com.neuralhealer.backend.feature.auth.repository.UserRepository;
import com.neuralhealer.backend.shared.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication controller handling user registration and login.
 * 
 * Endpoints:
 * - POST /api/auth/register - Register new user
 * - POST /api/auth/login - Login and get JWT token
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController {

        private final AuthService authService;
        private final OtpService otpService;
        private final UserRepository userRepository;

        /**
         * Register a new user.
         * Creates user account and role-specific profile (Doctor or Patient).
         * 
         * @param request Registration details
         * @return JWT token and user info
         */
        @PostMapping("/register")
        @Operation(summary = "Register a new user", description = "Creates a new user account with role-specific profile")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Registration successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input or email already exists"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<Map<String, Object>> register(
                        @Valid @RequestBody RegisterRequest request,
                        jakarta.servlet.http.HttpServletRequest requestContext,
                        jakarta.servlet.http.HttpServletResponse response) {
                log.info("Registration request for email: {}", request.email());

                AuthResponse authResponse = authService.register(request, response);

                // Trigger OTP email processing
                String ipAddress = requestContext.getRemoteAddr();
                String userAgent = requestContext.getHeader("User-Agent");
                userRepository.findByEmailAndDeletedAtIsNull(request.email())
                                .ifPresent(user -> authService.postRegisterProcessing(user, ipAddress, userAgent));

                // Return in format expected by frontend
                return ResponseEntity.ok(Map.of(
                                "data", authResponse,
                                "message",
                                "Registration successful. Please check your email for the verification code."));
        }

        /**
         * Login with email and password.
         * Returns JWT token for authenticated requests.
         * 
         * @param request Login credentials
         * @return JWT token and user info
         */
        @PostMapping("/login")
        @Operation(summary = "User login", description = "Authenticate user and return JWT token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<Map<String, Object>> login(
                        @Valid @RequestBody LoginRequest request,
                        jakarta.servlet.http.HttpServletResponse response) {
                log.info("Login request for email: {}", request.email());

                AuthResponse authResponse = authService.login(request, response);

                // Return in format expected by frontend
                return ResponseEntity.ok(Map.of(
                                "data", authResponse,
                                "message", "Login successful"));
        }

        /**
         * Logout user by clearing the authentication cookie.
         */
        @PostMapping("/logout")
        @Operation(summary = "Logout user", description = "Clears the authentication cookie")
        public ResponseEntity<Map<String, Object>> logout(jakarta.servlet.http.HttpServletResponse response) {
                jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("neuralhealer_token", null);
                cookie.setHttpOnly(true);
                cookie.setSecure(false);
                cookie.setPath("/api");
                cookie.setMaxAge(0); // Delete cookie
                response.addCookie(cookie);

                return ResponseEntity.ok(Map.of("message", "Logout successful"));
        }

        /**
         * Verify email with OTP.
         */
        @PostMapping("/verify-email")
        @Operation(summary = "Verify email", description = "Verify user email using the 6-digit OTP code sent via email")
        public ResponseEntity<Map<String, Object>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
                log.info("Email verification request for: {}", request.email());
                otpService.verifyOtp(request.email(), request.otpCode());
                return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
        }

        /**
         * Resend verification OTP.
         */
        @PostMapping("/resend-otp")
        @Operation(summary = "Resend OTP", description = "Resend a new verification code to the user's email")
        public ResponseEntity<Map<String, Object>> resendOtp(
                        @Valid @RequestBody ResendOtpRequest request,
                        jakarta.servlet.http.HttpServletRequest requestContext) {
                log.info("Resend OTP request for: {}", request.email());
                User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                String ipAddress = requestContext.getRemoteAddr();
                String userAgent = requestContext.getHeader("User-Agent");
                authService.postRegisterProcessing(user, ipAddress, userAgent);

                return ResponseEntity.ok(Map.of("message", "New verification code sent to your email"));
        }

        /**
         * Check verification status.
         */
        @GetMapping("/verification-status/{email}")
        @Operation(summary = "Check verification status", description = "Check if a user's email is already verified")
        public ResponseEntity<Map<String, Object>> checkStatus(@PathVariable String email) {
                User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return ResponseEntity.ok(Map.of(
                                "verified", !user.getEmailVerificationRequired(),
                                "email", email));
        }
}
