package com.neuralhealer.backend.feature.ai.repository;

import com.neuralhealer.backend.feature.ai.entity.AiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiChatSessionRepository extends JpaRepository<AiChatSession, UUID> {

    Optional<AiChatSession> findByPatientIdAndIsActiveTrue(UUID patientId);

    List<AiChatSession> findByPatientIdOrderByStartedAtDesc(UUID patientId);

    @Modifying
    @Query("UPDATE AiChatSession s SET s.sessionTitle = :title WHERE s.id = :sessionId")
    void updateTitle(@Param("sessionId") UUID sessionId, @Param("title") String title);

    @Query("SELECT DISTINCT s FROM AiChatSession s WHERE s.patientId = :patientId AND (LOWER(s.sessionTitle) LIKE LOWER(CONCAT('%', :query, '%')) OR s.id IN (SELECT m.sessionId FROM AiChatMessage m WHERE LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')))) ORDER BY s.startedAt DESC")
    List<AiChatSession> searchSessions(@Param("patientId") UUID patientId, @Param("query") String query);
}
