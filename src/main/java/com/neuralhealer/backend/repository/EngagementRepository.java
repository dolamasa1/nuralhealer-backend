package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.Engagement;
import com.neuralhealer.backend.model.enums.EngagementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EngagementRepository extends JpaRepository<Engagement, UUID> {

    // Find active engagement for a doctor-patient pair to enforce "One Active" rule
    @Query("SELECT e FROM Engagement e WHERE e.doctor.user.id = :doctorId AND e.patient.user.id = :patientId AND e.status IN :statuses")
    Optional<Engagement> findActiveEngagement(@Param("doctorId") UUID doctorId, @Param("patientId") UUID patientId,
            @Param("statuses") List<EngagementStatus> statuses);

    @Query("SELECT e FROM Engagement e WHERE e.doctor.user.id = :doctorId")
    List<Engagement> findByDoctorUserId(@Param("doctorId") UUID doctorId);

    @Query("SELECT e FROM Engagement e WHERE e.patient.user.id = :patientId")
    List<Engagement> findByPatientUserId(@Param("patientId") UUID patientId);

    @Query("SELECT e FROM Engagement e WHERE e.doctor.user.id = :doctorId AND e.status = :status")
    List<Engagement> findByDoctorUserIdAndStatus(@Param("doctorId") UUID doctorId,
            @Param("status") EngagementStatus status);
}
