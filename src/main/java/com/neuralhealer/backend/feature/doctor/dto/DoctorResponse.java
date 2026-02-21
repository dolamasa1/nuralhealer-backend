package com.neuralhealer.backend.feature.doctor.dto;

import java.util.List;
import java.util.UUID;

public record DoctorResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String title,
        String bio,
        List<String> specialities,
        Integer experienceYears,
        String locationCity,
        String locationCountry,
        Boolean isVerified) {
}
