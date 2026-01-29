package com.neuralhealer.backend.integration.gmail;

import com.neuralhealer.backend.model.entity.MessageQueue;
import com.neuralhealer.backend.repository.MessageQueueRepository;
import com.neuralhealer.backend.notification.repository.NotificationRepository;
import com.neuralhealer.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;

    /**
     * Process pending email notifications every minute.
     * Queries message_queues table for EMAIL_NOTIFICATION jobs with
     * status='pending'
     * and sends them via Gmail SMTP.
     */
    @Scheduled(fixedRate = 15000) // Every 15 seconds
    @Transactional
    public void processPendingEmails() {
        log.debug("Polling for pending email notifications...");

        // Fetch pending email jobs
        List<MessageQueue> pendingJobs = messageQueueRepository.findByJobTypeAndStatus(
                "EMAIL_NOTIFICATION",
                "pending",
                PageRequest.of(0, BATCH_SIZE));

        if (pendingJobs.isEmpty()) {
            return;
        }

        log.info("Found {} pending email jobs to process", pendingJobs.size());

        int successCount = 0;
        int failureCount = 0;

        for (MessageQueue job : pendingJobs) {
            try {
                boolean success = processEmailJob(job);
                if (success) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("Unexpected error processing email job {}: {}", job.getId(), e.getMessage(), e);
                handleJobFailure(job, "Unexpected error: " + e.getMessage());
                failureCount++;
            }
        }

        log.info("Email queue processing completed: {} successful, {} failed", successCount, failureCount);
    }

    /**
     * Process a single email job from the queue.
     *
     * @param job The message queue job to process
     * @return true if email was sent successfully, false otherwise
     */
    private boolean processEmailJob(MessageQueue job) {
        Map<String, Object> payload = job.getPayload();

        // Extract required fields from payload
        String recipientEmail = extractString(payload, "recipientEmail");
        String title = extractString(payload, "title");
        String body = extractString(payload, "body");

        // Fallback: Try to get email from userId if recipientEmail is missing
        if (recipientEmail == null || recipientEmail.isBlank()) {
            recipientEmail = getUserEmail(payload);
        }

        // Validate payload
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.error("Job {} missing recipientEmail and valid userId in payload", job.getId());
            handleJobFailure(job, "Invalid payload: missing recipient email");
            return false;
        }

        if (title == null || title.isBlank()) {
            log.warn("Job {} has empty title, using default", job.getId());
            title = "Notification";
        }

        if (body == null || body.isBlank()) {
            log.error("Job {} missing body in payload", job.getId());
            handleJobFailure(job, "Invalid payload: missing body");
            return false;
        }

        // Send email via Gmail SMTP
        try {
            // Check if this is a template-based notification
            String templateKey = extractString(payload, "templateKey");
            String emailBody = body;

            // Get user name for personalization
            String userName = extractString(payload, "userName");
            if (userName == null) {
                // Fallback: extract from userId
                String userEmail = getUserEmail(payload);
                if (userEmail != null) {
                    userName = userEmail.split("@")[0];
                }
            }
            if (userName == null)
                userName = "User";

            // Render appropriate template based on templateKey
            String doctorName = extractString(payload, "doctorName");

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

            gmailSmtpService.sendEmail(recipientEmail, title, emailBody);
            handleJobSuccess(job);
            return true;
        } catch (Exception e) {
            handleJobFailure(job, e.getMessage());
            return false;
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
     * Mark job as completed and update notification delivery status.
     */
    private void handleJobSuccess(MessageQueue job) {
        job.setStatus("completed");
        job.setProcessedAt(LocalDateTime.now());
        job.setErrorMessage(null);
        messageQueueRepository.save(job);

        // Update notification delivery status
        String notificationId = extractString(job.getPayload(), "notificationId");
        if (notificationId != null) {
            try {
                java.util.UUID uuid = java.util.UUID.fromString(notificationId);
                if (uuid == null)
                    return;
                notificationRepository.findById(uuid).ifPresent(notification -> {
                    java.util.Map<String, Object> status = notification.getDeliveryStatus();
                    if (status == null) {
                        status = new java.util.HashMap<>();
                    }
                    status.put("email", true);
                    notification.setDeliveryStatus(status);
                    notificationRepository.save(notification);
                    log.debug("Updated delivery status for notification {}", uuid);
                });
            } catch (Exception e) {
                log.warn("Failed to update notification delivery status: {}", e.getMessage());
            }
        }
        log.debug("Job {} marked as completed", job.getId());
    }

    /**
     * Handle job failure with retry logic.
     */
    private void handleJobFailure(MessageQueue job, String errorMessage) {
        int currentRetryCount = job.getRetryCount() != null ? job.getRetryCount() : 0;
        job.setRetryCount(currentRetryCount + 1);
        job.setErrorMessage(errorMessage);

        if (job.getRetryCount() >= MAX_RETRIES) {
            job.setStatus("failed");
            job.setProcessedAt(LocalDateTime.now());
            log.error("Job {} failed after {} retries: {}", job.getId(), MAX_RETRIES, errorMessage);
        } else {
            // Keep status as 'pending' for retry
            job.setStatus("pending");
            log.warn("Job {} failed (retry {}/{}): {}", job.getId(), job.getRetryCount(), MAX_RETRIES, errorMessage);
        }

        messageQueueRepository.save(job);
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
