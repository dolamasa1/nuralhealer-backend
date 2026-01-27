package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.MessageQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageQueueRepository extends JpaRepository<MessageQueue, UUID> {
}
