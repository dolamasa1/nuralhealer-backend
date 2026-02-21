ackage com.neuralhealer.backend.feature.engagement.repository.EngagementAccessRuleRepository;

import com.neuralhealer.backend.feature.engagement.entity.EngagementAccessRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EngagementAccessRuleRepository extends JpaRepository<EngagementAccessRule, java.util.UUID> {
    Optional<EngagementAccessRule> findByRuleName(String ruleName);
}
