ackage com.neuralhealer.backend.feature.auth.dto.AuthResponse;

import com.neuralhealer.backend.shared.entity.UserRole;
import lombok.Builder;

import java.util.UUID;

/**
 * Response DTO for authentication operations.
 * Contains JWT token and basic user information.
 * 
 * Used by: POST /api/auth/login, POST /api/auth/register
 */
@Builder
public record AuthResponse(
        String token,
        String type,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        long expiresIn) {
    // Default token type is Bearer
    public AuthResponse {
        if (type == null) {
            type = "Bearer";
        }
    }

    /**
     * Create AuthResponse for successful authentication.
     */
    public static AuthResponse of(String token, UUID userId, String email,
            String firstName, String lastName,
            UserRole role, long expiresIn) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .expiresIn(expiresIn)
                .build();
    }
}
