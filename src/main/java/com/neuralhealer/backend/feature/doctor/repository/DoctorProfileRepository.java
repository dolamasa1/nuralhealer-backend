ackage com.neuralhealer.backend.feature.doctor.repository.DoctorProfileRepository;

import com.neuralhealer.backend.feature.doctor.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DoctorProfile entity operations.
 */
@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<DoctorProfile> {

    /**
     * Find doctor profile by user ID.
     */
    Optional<DoctorProfile> findByUserId(UUID userId);

    /**
     * Check if doctor profile exists for user.
     */
    boolean existsByUserId(UUID userId);
}
