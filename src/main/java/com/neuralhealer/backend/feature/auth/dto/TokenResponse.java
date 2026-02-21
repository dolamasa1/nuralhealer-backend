package com.neuralhealer.backend.feature.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for token-related operations (retrieve, refresh).
 * 
 * @param engagementId The engagement this token belongs to
 * @param token        The token string (e.g., "NH-12345678")
 * @param expiresAt    When the token expires
 * @param qrCodeData   Optional QR code data for token
 * @param isNew        Whether this token was freshly generated (true) or
 *                     existing (false)
 */
public record TokenResponse(
                UUID engagementId,
                String token,
                LocalDateTime expiresAt,
                String qrCodeData,
                boolean isNew) {
}
