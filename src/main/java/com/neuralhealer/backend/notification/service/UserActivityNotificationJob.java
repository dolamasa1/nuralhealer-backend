package com.neuralhealer.backend.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuralhealer.backend.model.entity.SystemSetting;
import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.notification.entity.NotificationPriority;
import com.neuralhealer.backend.notification.entity.NotificationSource;
import com.neuralhealer.backend.notification.entity.NotificationType;
import com.neuralhealer.backend.notification.model.LocalizedMessage;
import com.neuralhealer.backend.repository.SystemSettingRepository;
import com.neuralhealer.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActivityNotificationJob {

    private final UserRepository userRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final NotificationService notificationService;
    private final com.neuralhealer.backend.notification.repository.NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Daily job at 9 AM to check user activity and send localized notifications.
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void checkUserActivity() {
        log.info("Starting User Activity Notification Job...");

        // Thresholds
        int reengageDays = getThreshold("notification_active_user_threshold_days", 3);
        int warningDays = getThreshold("notification_inactive_warning_days", 14);
        int inactiveDays = getThreshold("notification_inactive_status_days", 4);
        int totalInactiveDays = warningDays + inactiveDays;

        // 1. Re-engagement (active users, 3 days)
        processReEngagement(reengageDays);

        // 2. Inactivity Warning (dormant users, 14 days)
        processInactivityWarnings(warningDays);

        // 3. Mark as Inactive (18 days)
        markUsersInactive(totalInactiveDays);

        log.info("User Activity Notification Job completed.");
    }

    private void processReEngagement(int days) {
        List<User> users = userRepository.findUsersForReEngagement(days);
        log.info("Found {} users for re-engagement ({} days)", users.size(), days);

        for (User user : users) {
            sendLocalizedNotification(user, "USER_REENGAGE_ACTIVE", "patient");
            user.setActivityStatus("dormant");
            user.setLastActivityCheck(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    private void processInactivityWarnings(int days) {
        List<User> users = userRepository.findUsersForInactivityWarning(days);
        log.info("Found {} users for inactivity warning ({} days)", users.size(), days);

        for (User user : users) {
            sendLocalizedNotification(user, "USER_INACTIVITY_WARNING", "patient");
            user.setLastActivityCheck(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    private void markUsersInactive(int totalDays) {
        List<User> users = userRepository.findUsersToBeMarkedInactive(totalDays);
        log.info("Found {} users to mark as inactive ({} days)", users.size(), totalDays);

        for (User user : users) {
            user.setActivityStatus("inactive");
            user.setLastActivityCheck(LocalDateTime.now());
            userRepository.save(user);
            log.debug("User {} marked as inactive due to {} days of inactivity", user.getId(), totalDays);
        }
    }

    private void sendLocalizedNotification(User user, String templateKey, String context) {
        try {
            String placeholders = objectMapper.writeValueAsString(Map.of("userName", user.getFirstName()));
            LocalizedMessage msg = notificationRepository.getLocalizedMessage(templateKey, user.getId(), context,
                    placeholders);

            if (msg != null && msg.getTitle() != null) {
                notificationService.createNotification(
                        user.getId(),
                        NotificationType.SYSTEM_ALERT, // Fallback type if needed, but we can add new enum types
                        msg.getTitle(),
                        msg.getMessage(),
                        NotificationPriority.valueOf(msg.getPriority().toLowerCase()),
                        NotificationSource.system,
                        Map.of("userName", user.getFirstName(), "templateKey", templateKey));
            }
        } catch (Exception e) {
            log.error("Failed to send localized notification {} to user {}: {}", templateKey, user.getId(),
                    e.getMessage());
        }
    }

    private int getThreshold(String key, int defaultValue) {
        return systemSettingRepository.findByKey(key)
                .map(s -> {
                    try {
                        // Strip quotes if stored as JSON string "3"
                        String val = s.getValue().replaceAll("\"", "");
                        return Integer.parseInt(val);
                    } catch (Exception e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }
}
