ackage com.neuralhealer.backend.feature.notification.service.NotificationCleanupJob;

import com.neuralhealer.backend.feature.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupJob {

    private final NotificationRepository notificationRepository;

    /**
     * Daily cleanup task running at 2 AM.
     * Rules:
     * - Delete read notifications older than 30 days.
     * - Delete unread notifications older than 90 days.
     * - Delete expired notifications.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupNotifications() {
        log.info("Starting daily notification cleanup job...");

        LocalDateTime readThreshold = LocalDateTime.now().minusDays(30);
        LocalDateTime unreadThreshold = LocalDateTime.now().minusDays(90);
        LocalDateTime now = LocalDateTime.now();

        int readDeleted = notificationRepository.deleteReadOlderThan(readThreshold);
        int unreadDeleted = notificationRepository.deleteUnreadOlderThan(unreadThreshold);
        int expiredDeleted = notificationRepository.deleteExpired(now);

        log.info("Notification cleanup completed. Deleted: {} read, {} unread, {} expired.",
                readDeleted, unreadDeleted, expiredDeleted);
    }
}
