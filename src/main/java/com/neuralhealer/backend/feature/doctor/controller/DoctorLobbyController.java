package com.neuralhealer.backend.feature.doctor.controller;

import com.neuralhealer.backend.feature.doctor.dto.DoctorLobbyCardDTO;
import com.neuralhealer.backend.feature.doctor.dto.DoctorLobbyFilterRequest;
import com.neuralhealer.backend.feature.doctor.service.DoctorLobbyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctor Lobby", description = "Endpoints for browsing and searching doctors")
public class DoctorLobbyController {

    private final DoctorLobbyService doctorLobbyService;

    @GetMapping("/lobby")
    @Operation(summary = "Get doctor lobby", description = "Returns a paginated list of doctors with optional filters")
    public ResponseEntity<Page<DoctorLobbyCardDTO>> getDoctorLobby(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String verificationStatus,
            @RequestParam(required = false) String availabilityStatus,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "rating") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(50) int size) {

        DoctorLobbyFilterRequest filters = DoctorLobbyFilterRequest.builder()
                .specialization(specialization)
                .verificationStatus(verificationStatus)
                .availabilityStatus(availabilityStatus)
                .minRating(minRating)
                .location(location)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(doctorLobbyService.getDoctorLobby(filters));
    }

    @GetMapping("/search")
    @Operation(summary = "Search doctors", description = "Search doctors by name, title, bio, or specialization")
    public ResponseEntity<Page<DoctorLobbyCardDTO>> searchDoctors(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(50) int size) {

        return ResponseEntity.ok(doctorLobbyService.searchDoctors(q, PageRequest.of(page, size)));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get nearby doctors", description = "Find doctors within a specific radius based on geolocation")
    public ResponseEntity<List<DoctorLobbyCardDTO>> getNearbyDoctors(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") @Max(100) int radius) {

        return ResponseEntity.ok(doctorLobbyService.getNearbyDoctors(lat, lng, radius));
    }
}
