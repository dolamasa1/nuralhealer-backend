package com.neuralhealer.backend.controller;

import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.model.enums.UserRole;
import com.neuralhealer.backend.repository.DoctorProfileRepository;
import com.neuralhealer.backend.repository.PatientProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * User controller for authenticated user operations.
 * 
 * Endpoints:
 * - GET /api/users/me - Get current authenticated user info
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;

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
