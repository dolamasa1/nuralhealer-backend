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

    @GetMapping
    @Operation(summary = "Get all doctors", description = "List all available doctor profiles")
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        List<DoctorResponse> doctors = doctorProfileRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(doctors);
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
