ackage com.neuralhealer.backend.feature.patient.dto.SessionWithDoctorsResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Enriched session response that includes authorized doctors who can view this
 * chat
 */
public record SessionWithDoctorsResponse(
        UUID sessionId,
        String sessionTitle,
        String sessionType,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Boolean isActive,
        Integer messageCount,
        List<DoctorBasicInfo> authorizedDoctors) {
    /**
     * Basic doctor information for session access
     */
    public record DoctorBasicInfo(
            UUID doctorId,
            String fullName,
            String title,
            List<String> specialities,
            String accessLevel,
            boolean isCurrentlyActive) {
    }
}
