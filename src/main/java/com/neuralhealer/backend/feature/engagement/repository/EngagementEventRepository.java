ackage com.neuralhealer.backend.feature.engagement.repository.EngagementEventRepository;

import com.neuralhealer.backend.feature.engagement.entity.EngagementEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EngagementEventRepository extends JpaRepository<EngagementEvent, UUID> {
    List<EngagementEvent> findByEngagementIdOrderByTriggeredAtDesc(UUID engagementId);
}
