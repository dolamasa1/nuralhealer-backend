ackage com.neuralhealer.backend.feature.patient.repository.PatientProfileRepository;

import com.neuralhealer.backend.feature.patient.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PatientProfile entity operations.
 */
@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID> {

    /**
     * Find patient profile by user ID.
     */
    Optional<PatientProfile> findByUserId(UUID userId);

    /**
     * Check if patient profile exists for user.
     */
    boolean existsByUserId(UUID userId);
}
