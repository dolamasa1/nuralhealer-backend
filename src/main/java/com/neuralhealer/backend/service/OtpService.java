package com.neuralhealer.backend.service;

import com.neuralhealer.backend.model.entity.EmailVerificationOtp;
import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.notification.entity.Notification;
import com.neuralhealer.backend.notification.entity.NotificationPriority;
import com.neuralhealer.backend.notification.entity.NotificationSource;
import com.neuralhealer.backend.notification.entity.NotificationType;
import com.neuralhealer.backend.notification.repository.NotificationRepository;
import com.neuralhealer.backend.repository.EmailVerificationOtpRepository;
import com.neuralhealer.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing OTP-based email verification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final EmailVerificationOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final com.neuralhealer.backend.integration.gmail.DirectEmailService directEmailService;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int OTP_EXPIRY_MINUTES = 30;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_LIMIT_PER_HOUR = 3;

    /**
     * Generate and send a new OTP for a user.
     */
    @Transactional
    public void generateAndSendOtp(User user, String ipAddress, String userAgent) {
        log.info("=== OTP GENERATION START === User: {}, ID: {}", user.getEmail(), user.getId());

        // Enforce rate limiting
        long recentOtps = otpRepository.countByUserIdAndCreatedAtAfter(user.getId(), LocalDateTime.now().minusHours(1));
        log.info("Recent OTPs count for user {}: {}", user.getEmail(), recentOtps);

        if (recentOtps >= RESEND_LIMIT_PER_HOUR) {
            log.warn("Rate limit exceeded for OTP requests: user={}", user.getEmail());
            throw new RuntimeException("Too many OTP requests. Please wait an hour before trying again.");
        }

        // Invalidate old OTPs
        int invalidated = otpRepository.invalidateAllForUser(user.getId());
        log.info("Invalidated {} old OTPs for user: {}", invalidated, user.getEmail());

        // Generate 6-digit code
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        log.info("Generated OTP code: {} for user: {}", code, user.getEmail());

        // Save new OTP
        EmailVerificationOtp otp = EmailVerificationOtp.builder()
                .user(user)
                .otpCode(code)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        log.info("Saving OTP to database - Code: {}, Expires: {}", code, otp.getExpiresAt());
        EmailVerificationOtp savedOtp = otpRepository.save(otp);
        otpRepository.flush(); // Force immediate write to DB
        log.info("OTP SAVED TO DATABASE - ID: {}, Code: {}, User: {}", savedOtp.getId(), savedOtp.getOtpCode(),
                user.getEmail());

        // Queue notification - this will be picked up by EmailQueueProcessor
        queueOtpNotification(user, code);

        // Update user status
        user.setEmailVerificationSentAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("=== OTP GENERATION COMPLETE === User: {}, Code: {}", user.getEmail(), code);
    }

    /**
     * Verify the provided OTP code.
     */
    @Transactional
    public void verifyOtp(String email, String code) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check verification lockout
        if (user.getVerificationLockedUntil() != null
                && user.getVerificationLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Account temporarily locked due to too many failed attempts. Try again later.");
        }

        Optional<EmailVerificationOtp> otpOpt = otpRepository.findValidOtpByUserId(user.getId(), LocalDateTime.now());

        // Diagnostic logging
        log.info("OTP Verification Debug - User: {}, Provided Code: {}, OTP Found: {}",
                email, code, otpOpt.isPresent());

        if (otpOpt.isPresent()) {
            EmailVerificationOtp foundOtp = otpOpt.get();
            log.info("OTP Details - Stored Code: {}, Expires At: {}, Is Used: {}, Attempts: {}",
                    foundOtp.getOtpCode(), foundOtp.getExpiresAt(), foundOtp.getIsUsed(), foundOtp.getAttempts());
            log.info("Code Match: {}", foundOtp.getOtpCode().equals(code));
        }

        if (otpOpt.isEmpty() || !otpOpt.get().getOtpCode().equals(code)) {
            handleFailedAttempt(user, otpOpt.orElse(null));

            String reason = otpOpt.isEmpty()
                    ? "No valid OTP found for user"
                    : "OTP code mismatch - provided: " + code + ", expected: " + otpOpt.get().getOtpCode();
            log.warn("OTP verification failed for {}: {}", email, reason);

            throw new RuntimeException("Invalid or expired verification code");
        }

        EmailVerificationOtp otp = otpOpt.get();

        // Mark as verified
        otp.setIsUsed(true);
        otp.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(otp);

        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setEmailVerificationRequired(false);
        user.setFailedVerificationAttempts(0);
        user.setVerificationLockedUntil(null);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", email);
    }

    private void handleFailedAttempt(User user, EmailVerificationOtp otp) {
        if (otp != null) {
            otp.incrementAttempts();
            if (otp.getAttempts() >= MAX_ATTEMPTS) {
                otp.setIsUsed(true); // Invalidate if max attempts reached
            }
            otpRepository.save(otp);
        }

        int failedAttempts = (user.getFailedVerificationAttempts() == null ? 0 : user.getFailedVerificationAttempts())
                + 1;
        user.setFailedVerificationAttempts(failedAttempts);

        if (failedAttempts >= MAX_ATTEMPTS) {
            user.setVerificationLockedUntil(LocalDateTime.now().plusMinutes(30));
            log.warn("Account locked for verification: {}", user.getEmail());
        }
        userRepository.save(user);
    }

    private void queueOtpNotification(User user, String code) {
        // Send email directly - no queue, no triggers, no complexity
        try {
            String userName = user.getFirstName() != null && !user.getFirstName().isBlank()
                    ? user.getFirstName()
                    : "User";
            directEmailService.sendOtpVerificationEmail(user.getEmail(), userName, code);
            log.info("OTP verification email sent directly to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again.");
        }

        // Still create notification for in-app display (SSE only)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("otpCode", code);
        metadata.put("userName", user.getFirstName());

        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.EMAIL_VERIFICATION)
                .title("Verify Your Email Address")
                .message("Your verification code is: " + code)
                .payload(metadata)
                .priority(NotificationPriority.high)
                .source(NotificationSource.system)
                .sendEmail(false) // Email already sent directly
                .deliveryStatus(Map.of("sse", true, "email", true))
                .build();

        notificationRepository.save(notification);
    }
}
