package com.neuralhealer.backend.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for authorized doctors who can view patient chat history
 */
public record AuthorizedDoctorResponse(
                UUID doctorId,
                String fullName,
                String title,
                List<String> specialities,
                String accessLevel,
                boolean isCurrentlyActive) {
}
