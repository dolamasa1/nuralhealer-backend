package com.neuralhealer.backend.integration.gmail;

import com.neuralhealer.backend.model.entity.MessageQueue;
import com.neuralhealer.backend.repository.MessageQueueRepository;
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
 * Runs every 1 minute to send queued emails via Gmail SMTP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailQueueProcessor {

    private final MessageQueueRepository messageQueueRepository;
    private final UserRepository userRepository;
    private final GmailSmtpService gmailSmtpService;

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;

    /**
     * Process pending email notifications every minute.
     * Queries message_queues table for EMAIL_NOTIFICATION jobs with
     * status='pending'
     * and sends them via Gmail SMTP.
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    @Transactional
    public void processPendingEmails() {
        log.debug("Starting email queue processing...");

        // Fetch pending email jobs
        List<MessageQueue> pendingJobs = messageQueueRepository.findByJobTypeAndStatus(
                "EMAIL_NOTIFICATION",
                "pending",
                PageRequest.of(0, BATCH_SIZE));

        if (pendingJobs.isEmpty()) {
            log.debug("No pending email jobs to process");
            return;
        }

        log.info("Processing {} pending email jobs", pendingJobs.size());

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

        // Validate payload
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.error("Job {} missing recipientEmail in payload", job.getId());
            handleJobFailure(job, "Invalid payload: missing recipientEmail");
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
            gmailSmtpService.sendEmail(recipientEmail, title, body);
            handleJobSuccess(job);
            return true;
        } catch (Exception e) {
            handleJobFailure(job, e.getMessage());
            return false;
        }
    }

    /**
     * Mark job as completed.
     */
    private void handleJobSuccess(MessageQueue job) {
        job.setStatus("completed");
        job.setProcessedAt(LocalDateTime.now());
        job.setErrorMessage(null);
        messageQueueRepository.save(job);
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
            return userRepository.findById(userId)
                    .map(user -> user.getEmail())
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to extract user email from userId: {}", e.getMessage());
            return null;
        }
    }
}
