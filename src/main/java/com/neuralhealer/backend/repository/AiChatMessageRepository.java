package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, UUID> {

    List<AiChatMessage> findBySessionIdOrderBySentAt(UUID sessionId);
}
