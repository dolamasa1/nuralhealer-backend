package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.EngagementAccessRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EngagementAccessRuleRepository extends JpaRepository<EngagementAccessRule, String> {
    Optional<EngagementAccessRule> findByRuleName(String ruleName);
}
