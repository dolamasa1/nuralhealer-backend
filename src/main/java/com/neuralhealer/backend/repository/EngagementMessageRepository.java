package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.EngagementMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EngagementMessageRepository extends JpaRepository<EngagementMessage, UUID> {
    List<EngagementMessage> findByEngagementIdOrderBySentAtAsc(UUID engagementId);
}
