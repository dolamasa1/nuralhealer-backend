ackage com.neuralhealer.backend.shared.entity.UserRole;

/**
 * User roles in the NeuralHealer platform.
 * Maps to application-level authorization, not stored in DB as enum.
 */
public enum UserRole {
    PATIENT,
    DOCTOR,
    ADMIN
}
