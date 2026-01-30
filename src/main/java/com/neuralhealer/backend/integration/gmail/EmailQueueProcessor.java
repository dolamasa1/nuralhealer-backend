package com.neuralhealer.backend.integration.gmail;

import com.neuralhealer.backend.model.entity.MessageQueue;
import com.neuralhealer.backend.repository.MessageQueueRepository;
import com.neuralhealer.backend.notification.repository.NotificationRepository;
import com.neuralhealer.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled job that processes the message_queues table for email
 * notifications.
 * Runs every 15 seconds to send queued emails via Gmail SMTP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailQueueProcessor {

    private final MessageQueueRepository messageQueueRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final GmailSmtpService gmailSmtpService;
    private final ScheduledExecutorService simpleScheduler = Executors.newSingleThreadScheduledExecutor();
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;

    // Prevent scheduling same job multiple times in memory
    private final Map<UUID, Boolean> scheduledRetries = new ConcurrentHashMap<>();

    /**
     * Triggered by PostgresNotificationListener when a NEW email job is created.
     * This is purely event-driven. No polling.
     */
    // @Scheduled removed - purely event driven as requested
    public void processPendingEmails() {
        log.debug("📬 Polling for pending email notifications...");

        // Fetch pending email jobs (no transactional needed for read)
        List<MessageQueue> pendingJobs = messageQueueRepository.findByJobTypeAndStatus(
                "EMAIL_NOTIFICATION",
                "pending",
                PageRequest.of(0, BATCH_SIZE));

        if (pendingJobs.isEmpty()) {
            return;
        }

        log.info("📧 Found {} pending email jobs to process", pendingJobs.size());

        int successCount = 0;
        int failureCount = 0;

        for (MessageQueue job : pendingJobs) {
            try {
                // Process each job in its own transaction context
                boolean result = processSingleJob(job);
                if (result) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("❌ Unexpected error for job {}: {}", job.getId(), e.getMessage());
                failureCount++;
            }
        }

        log.info("✅ Batch completed: {} successful, {} failed", successCount, failureCount);
    }

    /**
     * Process a single job with explicit transaction management.
     */
    public boolean processSingleJob(MessageQueue job) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            // 1. Atomically lock the job
            if (messageQueueRepository.updateStatusIfPending(job.getId(), "processing", "pending") == 0) {
                log.debug("⏭️ Job {} already processing, skipping", job.getId());
                return false;
            }

            try {
                boolean success = processEmailJob(job);
                if (success) {
                    messageQueueRepository.markAsCompleted(job.getId());
                    log.info("✅ Job {} completed", job.getId());
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                log.error("❌ Error in processEmailJob {}: {}", job.getId(), e.getMessage());
                handleJobFailure(job, e.getMessage());
                return false;
            }
        }));
    }

    /**
     * Process a single email job from the queue.
     *
     * @param job The message queue job to process (must already be locked)
     * @return true if email was sent successfully, false otherwise
     */
    private boolean processEmailJob(MessageQueue job) {
        Map<String, Object> payload = job.getPayload();
        String recipientEmail = extractString(payload, "recipientEmail");

        if (recipientEmail == null || recipientEmail.isBlank()) {
            recipientEmail = getUserEmail(payload);
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            handleJobFailure(job, "Missing recipientEmail");
            return false;
        }

        String title = COALESCE(extractString(payload, "title"), "Notification");
        String body = extractString(payload, "body");

        if (body == null || body.isBlank()) {
            handleJobFailure(job, "Missing body");
            return false;
        }

        try {
            renderAndSendEmail(recipientEmail, title, body, payload, job.getId());
            updateNotificationStatus(payload);
            return true;
        } catch (Exception e) {
            handleJobFailure(job, e.getMessage());
            return false;
        }
    }

    private String COALESCE(String val, String def) {
        return (val == null || val.isBlank()) ? def : val;
    }

    private void renderAndSendEmail(String email, String title, String body, Map<String, Object> payload, UUID jobId) {
        String templateKey = extractString(payload, "templateKey");
        String userName = COALESCE(extractString(payload, "userName"), "User");
        String doctorName = extractString(payload, "doctorName");

        String emailBody = body;
        if ("USER_WELCOME".equals(templateKey)) {
            emailBody = renderTemplate("welcome.html", userName, null);
        } else if ("USER_REENGAGE_ACTIVE".equals(templateKey)) {
            emailBody = renderTemplate("re-engage.html", userName, null);
        } else if ("USER_INACTIVITY_WARNING".equals(templateKey)) {
            emailBody = renderTemplate("inactivity-warning.html", userName, null);
        } else if ("ENGAGEMENT_STARTED".equals(templateKey)) {
            emailBody = renderTemplate("engagement-started.html", userName, doctorName);
        } else if ("ENGAGEMENT_CANCELLED".equals(templateKey)) {
            emailBody = renderTemplate("engagement-cancelled.html", userName, doctorName);
        }

        log.info("📤 Sending email to {} for job {}", email, jobId);
        gmailSmtpService.sendEmail(email, title, emailBody);
        log.info("✅ Email sent successfully to {}", email);
    }

    private void updateNotificationStatus(Map<String, Object> payload) {
        String notificationId = extractString(payload, "notificationId");
        if (notificationId != null) {
            try {
                notificationRepository.findById(UUID.fromString(notificationId)).ifPresent(notification -> {
                    Map<String, Object> status = notification.getDeliveryStatus();
                    if (status == null)
                        status = new java.util.HashMap<>();
                    status.put("email", true);
                    notification.setDeliveryStatus(status);
                    notificationRepository.save(notification);
                });
            } catch (Exception e) {
                log.warn("Non-critical: Failed to update notification delivery status: {}", e.getMessage());
            }
        }
    }

    /**
     * Render email template with user name and doctor name replacement.
     */
    private String renderTemplate(String templateName, String userName, String doctorName) {
        try (var is = getClass().getClassLoader().getResourceAsStream("templates/emails/" + templateName)) {
            if (is == null) {
                log.warn("{} template not found, using fallback", templateName);
                return getFallbackTemplate(userName);
            }
            String template = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            template = template.replace("{USER_NAME}", userName);
            if (doctorName != null) {
                template = template.replace("{DOCTOR_NAME}", doctorName);
            }
            return template;
        } catch (Exception e) {
            log.error("Error loading {} template: {}", templateName, e.getMessage());
            return getFallbackTemplate(userName);
        }
    }

    /**
     * Fallback email template.
     */
    private String getFallbackTemplate(String userName) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f4f4f4;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px;">
                        <h2 style="color: #4CAF50;">Welcome to NeuralHealer, %s!</h2>
                        <p>Thank you for joining our healthcare platform.</p>
                        <p>We're excited to support you on your wellness journey.</p>
                        <p style="margin-top: 30px;">Best regards,<br/>The NeuralHealer Team</p>
                    </div>
                </body>
                </html>
                """
                .formatted(userName);
    }

    /**
     * Handle job failure with retry logic using native SQL and Event-Scheduled
     * logic.
     */
    private void handleJobFailure(MessageQueue job, String errorMessage) {
        int currentRetryCount = job.getRetryCount() != null ? job.getRetryCount() : 0;
        int nextRetryCount = currentRetryCount + 1;
        String nextStatus = (nextRetryCount >= MAX_RETRIES) ? "failed" : "pending";

        messageQueueRepository.updateStatusAndError(job.getId(), nextStatus, nextRetryCount, errorMessage);

        if ("failed".equals(nextStatus)) {
            log.error("💀 Job {} failed permanently: {}", job.getId(), errorMessage);
            scheduledRetries.remove(job.getId());
        } else {
            // Event-Scheduled Retry: Schedule a run in the future without polling DB
            // Exponential backoff: 30s, 60s, 120s...
            long delaySeconds = (long) Math.pow(2, nextRetryCount) * 15;
            log.warn("⏳ Job {} scheduled for retry ({}/{}) in {}s", job.getId(), nextRetryCount, MAX_RETRIES,
                    delaySeconds);

            if (!scheduledRetries.containsKey(job.getId())) {
                scheduledRetries.put(job.getId(), true);

                // Use internal scheduler instead of Spring bean to avoid conflicts
                simpleScheduler.schedule(() -> {
                    try {
                        scheduledRetries.remove(job.getId());
                        log.info("⏰ Waking up for retry of job {}", job.getId());
                        processSingleJob(job);
                    } catch (Exception e) {
                        log.error("Error during scheduled retry of {}: {}", job.getId(), e.getMessage());
                    }
                }, delaySeconds, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Safely extract string value from payload map.
     */
    private String extractString(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Get user email from userId in payload.
     * This is a fallback if recipientEmail is not in the payload.
     */
    private String getUserEmail(Map<String, Object> payload) {
        Object userIdObj = payload.get("userId");
        if (userIdObj == null) {
            return null;
        }

        try {
            java.util.UUID userId = java.util.UUID.fromString(userIdObj.toString());
            if (userId == null)
                return null;
            return userRepository.findById(userId)
                    .map(user -> user.getEmail())
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to extract user email from userId: {}", e.getMessage());
            return null;
        }
    }
}
