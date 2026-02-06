package com.neuralhealer.backend.controller;

import com.neuralhealer.backend.model.dto.DoctorResponse;
import com.neuralhealer.backend.model.entity.DoctorProfile;
import com.neuralhealer.backend.repository.DoctorProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors", description = "Doctor management and directory endpoints")
public class DoctorController {

    private final DoctorProfileRepository doctorProfileRepository;
    private final com.neuralhealer.backend.repository.DoctorPatientRepository doctorPatientRepository;
    private final com.neuralhealer.backend.service.ChatStorageService chatStorageService;

    @GetMapping
    @Operation(summary = "Get all doctors", description = "List all available doctor profiles")
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        List<DoctorResponse> doctors = doctorProfileRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/patients/{patientId}/chats")
    @Operation(summary = "Get patient chats", description = "View chat history for a specific patient")
    public List<com.neuralhealer.backend.model.entity.AiChatSession> getPatientChats(
            @org.springframework.web.bind.annotation.PathVariable java.util.UUID patientId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.neuralhealer.backend.model.entity.User user) {
        if (!user.isDoctor()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Only doctors can access this endpoint");
        }

        java.util.UUID doctorId = user.getDoctorProfile().getId();
        if (!doctorPatientRepository.existsByDoctorIdAndPatientId(doctorId, patientId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "You do not have access to this patient");
        }

        return chatStorageService.getUserSessions(patientId);
    }

    @GetMapping("/patients/{patientId}/chats/{sessionId}/messages")
    @Operation(summary = "Get patient chat messages", description = "View messages for a specific session")
    public List<com.neuralhealer.backend.model.entity.AiChatMessage> getPatientChatMessages(
            @org.springframework.web.bind.annotation.PathVariable java.util.UUID patientId,
            @org.springframework.web.bind.annotation.PathVariable java.util.UUID sessionId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.neuralhealer.backend.model.entity.User user) {
        if (!user.isDoctor()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Only doctors can access this endpoint");
        }

        java.util.UUID doctorId = user.getDoctorProfile().getId();
        if (!doctorPatientRepository.existsByDoctorIdAndPatientId(doctorId, patientId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "You do not have access to this patient");
        }

        return chatStorageService.getSessionMessages(sessionId);
    }

    private DoctorResponse mapToResponse(DoctorProfile p) {
        return new DoctorResponse(
                p.getId(),
                p.getUser().getId(),
                p.getUser().getFirstName(),
                p.getUser().getLastName(),
                p.getUser().getEmail(),
                p.getTitle(),
                p.getBio(),
                p.getSpecialities(),
                p.getExperienceYears(),
                p.getLocationCity(),
                p.getLocationCountry(),
                p.getIsVerified());
    }
}
