package com.neuralhealer.backend.controller;

import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.model.enums.UserRole;
import com.neuralhealer.backend.repository.DoctorProfileRepository;
import com.neuralhealer.backend.repository.PatientProfileRepository;
import com.neuralhealer.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * User controller for user related operations.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;

    /**
     * Get user information by email.
     * 
     * @param email User email to search for
     * @return User information
     */
    @GetMapping("/by-email")
    @Operation(summary = "Find user by email", description = "Returns user information given their email address")
    public ResponseEntity<Map<String, Object>> getUserByEmail(
            @Parameter(description = "Email address to search for") @RequestParam String email) {

        User foundUser = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + email));

        // Determine user role
        UserRole role = UserRole.PATIENT;
        if (doctorProfileRepository.existsByUserId(foundUser.getId())) {
            role = UserRole.DOCTOR;
        } else if (patientProfileRepository.existsByUserId(foundUser.getId())) {
            role = UserRole.PATIENT;
        }

        Map<String, Object> userData = Map.of(
                "id", foundUser.getId(),
                "email", foundUser.getEmail(),
                "firstName", foundUser.getFirstName(),
                "lastName", foundUser.getLastName(),
                "role", role,
                "phone", foundUser.getPhone() != null ? foundUser.getPhone() : "",
                "emailVerified", foundUser.isEmailVerified(),
                "createdAt", foundUser.getCreatedAt());

        return ResponseEntity.ok(Map.of(
                "data", userData,
                "message", "User found"));
    }

    /**
     * Get current authenticated user information.
     * Requires valid JWT token in Authorization header.
     * 
     * @param user Current authenticated user (injected by Spring Security)
     * @return User information
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the currently authenticated user's information")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal User user) {
        // Determine user role
        UserRole role = UserRole.PATIENT;
        if (doctorProfileRepository.existsByUserId(user.getId())) {
            role = UserRole.DOCTOR;
        } else if (patientProfileRepository.existsByUserId(user.getId())) {
            role = UserRole.PATIENT;
        }

        Map<String, Object> userData = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "role", role,
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "emailVerified", user.isEmailVerified(),
                "createdAt", user.getCreatedAt());

        return ResponseEntity.ok(Map.of(
                "data", userData,
                "message", "Success"));
    }
}
